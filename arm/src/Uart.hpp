/**
 * Uart.hpp
 * Handle UARTs (transmission/reception) for STM32F411XE
 *
 *********************************************************
 * Trois s�ries diff�rentes: USART1, USART 2 et USART6
 * Pins :
 *
 * UART1: A15 (TX) A10 (RX)
 * UART2: A2 (TX) A3 (RX)
 * UART6: C6 (TX) C7 (RX)
 *
 *
 * Jaune: TX
 * Orange: RX
 */

#ifndef UART_HPP
#define UART_HPP

#include "stm32f4xx.h"
#include "stm32f4xx_hal.h"
#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <stdarg.h>
#include "FreeRTOS.h"
#include "task.h"

#define RX_BUFFER_SIZE 128

	enum {
		READ_TIMEOUT = 0, READ_SUCCESS = 1
	};

template<uint8_t USART_ID>
class Uart {
public:
	static UART_HandleTypeDef UART;

	/**
	 * Write functions : send raw strings
	 *
	 */

	template<class T>

	static inline void write(char* val) {
		HAL_UART_Transmit(&UART, (uint8_t*) val, strlen(val), 100);
	}

	static inline void write(const char* val) {
		HAL_UART_Transmit(&UART, (uint8_t*) val, strlen(val), 100);
	}

	static inline void write(unsigned char* val, uint8_t longueur) {
		HAL_UART_Transmit(&UART, (uint8_t*) val, longueur, 100);
	}

	struct ring_buffer {
		unsigned char buffer[RX_BUFFER_SIZE];
		int head = 0;
		int tail = 0;
	};
	static volatile ring_buffer rx_buffer_;

	/**
	 * Initialize the UART  : set pins, enable clocks, set uart, enable interrupt
	 *
	 */
	static inline void init(uint32_t baudrate, uint32_t mode) {
		GPIO_InitTypeDef GPIO_InitStruct;

		//General settings of pins TX/RX
		GPIO_InitStruct.Mode = GPIO_MODE_AF_PP;
		GPIO_InitStruct.Pull = GPIO_PULLUP;
		GPIO_InitStruct.Speed = GPIO_SPEED_HIGH;

		switch (USART_ID) {
		case 1:
			UART.Instance = USART1;

			GPIO_InitStruct.Pin = GPIO_PIN_6 | GPIO_PIN_7; // Pins B6 (TX) and B7 (RX)
			GPIO_InitStruct.Alternate = GPIO_AF7_USART1;

			__HAL_RCC_GPIOB_CLK_ENABLE();
			__HAL_RCC_USART1_CLK_ENABLE();

			NVIC_SetPriority(USART1_IRQn, 1);
			NVIC_EnableIRQ(USART1_IRQn);

			HAL_GPIO_Init(GPIOB, &GPIO_InitStruct);
			break;
		case 2: // S�rie raspberry
			UART.Instance = USART2;

			GPIO_InitStruct.Pin = GPIO_PIN_5 | GPIO_PIN_6; // Pins D5 (TX) and D6 (RX)
			GPIO_InitStruct.Alternate = GPIO_AF7_USART2;

			__HAL_RCC_GPIOD_CLK_ENABLE();
			__HAL_RCC_USART2_CLK_ENABLE();

			NVIC_SetPriority(USART2_IRQn, 1);
			NVIC_EnableIRQ(USART2_IRQn);

			HAL_GPIO_Init(GPIOD, &GPIO_InitStruct);
			break;

		case 3: // S�rie AX12
			UART.Instance = USART3;
//			HAL_HalfDuplex_Init(&UART); // si un jour on a le temps de configurer correctement les AX12...
			GPIO_InitStruct.Pin = GPIO_PIN_10; // Pin B10 (TX)
			GPIO_InitStruct.Alternate = GPIO_AF7_USART3;

			__HAL_RCC_GPIOB_CLK_ENABLE();
			__HAL_RCC_USART3_CLK_ENABLE();

			NVIC_SetPriority(USART3_IRQn, 1);
			NVIC_EnableIRQ(USART3_IRQn);

			HAL_GPIO_Init(GPIOB, &GPIO_InitStruct);
			break;
		}

		//UART setting
		UART.Init.BaudRate = baudrate;
		UART.Init.WordLength = UART_WORDLENGTH_8B; // octet comme taille �l�mentaire (standard)
		UART.Init.StopBits = UART_STOPBITS_1; // bit de stop = 1 (standard)
		UART.Init.Parity = UART_PARITY_NONE; // pas de bit de parit� (standard)
		UART.Init.Mode = mode;

		if (HAL_UART_Init(&UART) != HAL_OK)
			while(1);

		__HAL_UART_ENABLE_IT(&UART, UART_IT_RXNE);
	}

	/**
	 * Base function to send only one byte
	 *
	 */
	static inline void send_char(unsigned char c) {
		HAL_UART_Transmit(&UART, (uint8_t*) &c, 1, 100);
	}

	/**
	 * Availability of data in the buffer
	 */
	static inline bool available(void)
	{
		return rx_buffer_.head != rx_buffer_.tail;
	}

	
	/**
	 * Read one byte from the ring buffer with a timeout (~ in ms)
	 *
	 */
	static inline uint8_t read_char(unsigned char *byte)
	{
		if(!available())
			vTaskDelay(1);
		if(!available())
			return READ_TIMEOUT;
		*byte = rx_buffer_.buffer[rx_buffer_.tail];
		rx_buffer_.tail = (rx_buffer_.tail + 1) % RX_BUFFER_SIZE;
		return READ_SUCCESS;
	}


	/**
	 * Store one byte in the ring buffer
	 *
	 */
	static inline void store_char(unsigned char c) {
		int i = (rx_buffer_.head + 1) % RX_BUFFER_SIZE;
		if (i != rx_buffer_.tail)
		{
			rx_buffer_.buffer[rx_buffer_.head] = c;
			rx_buffer_.head = i;
		}
	}

	static inline void printf(const char *format, ...)
	{
		va_list args;
		va_start(args, format);
		char buffer[64];
		vsnprintf(buffer, 64, format, args);
		write(buffer);
		va_end(args);
	}

	__attribute__((format(printf, 1, 0)))
	static inline void printfln(const char *format, ...)
	{
		va_list args;
		va_start(args, format);
		char buffer[64];
		vsnprintf(buffer, 64, format, args);
		write(buffer);
		write("\r\n");
		va_end(args);
	}

};

template<uint8_t ID>
volatile typename Uart<ID>::ring_buffer Uart<ID>::rx_buffer_;

#endif  /* UART_HPP */
