#ifndef TH_SERIE
#define TH_SERIE

#include <stdio.h>
#include <stdlib.h>
#include <vector>
#include "diag/Trace.h"

#include "math.h"
#include "Timer.h"
#include "FreeRTOS.h"
#include "task.h"

#include "asser.hpp"
#include "Hook.h"
#include "Uart.hpp"
#include "global.h"
#include "ax12.hpp"
#include "serialProtocol.h"
#include "serie.h"

using namespace std;

/**
 * Thread qui �coute la s�rie
 */
void thread_ecoute_serie(void*)
{
	/**
	 * Initialisation des s�ries
	 */
	serial_rb.init(460800, UART_MODE_TX_RX);
	serial_ax.init(57600, UART_MODE_TX);

	for(uint8_t i = 0; i < NB_AX12; i++)
		ax12[i] = new AX<Uart<3>>(i, 0, 1023);

//	ax12[0]->goToB(60);
//	vTaskDelay(3000);
//	ax12[0]->goToB(145);

	/*
	ax12[0]->goToB(300-0);
	vTaskDelay(3000);
	ax12[0]->goToB(300-45);
	vTaskDelay(3000);
	ax12[0]->goToB(300-100);
	vTaskDelay(3000);
	ax12[0]->goToB(300-135);
	vTaskDelay(3000);
	ax12[0]->goToB(300-210);
	vTaskDelay(3000);
	ax12[0]->goToB(300-216);*/
/*
	uint8_t idax = 3;

	serial_ax.init(1000000, UART_MODE_TX);
	AX<Uart<3>>* ax = new AX<Uart<3>>(idax, 0, 1023);
	vTaskDelay(200);
	serial_ax.send_char(0xFF); // set baudrate
	serial_ax.send_char(0xFF);
	serial_ax.send_char(0xFE);
	serial_ax.send_char(0x04);
	serial_ax.send_char(0x03);
	serial_ax.send_char(0x04);
	serial_ax.send_char(0x22);
	serial_ax.send_char(0xD4);

	vTaskDelay(200);
	serial_ax.init(57600, UART_MODE_TX);
	vTaskDelay(200);
	serial_ax.send_char(0xFF); // set id
	serial_ax.send_char(0xFF);
	serial_ax.send_char(0xFE);
	serial_ax.send_char(0x04);
	serial_ax.send_char(0x03);
	serial_ax.send_char(0x03);
	serial_ax.send_char(idax);
	serial_ax.send_char(0xF7-idax);

	vTaskDelay(200);
	ax->goTo(300);	// test.*/

	uint16_t idDernierPaquet = -1;
	Hook* hookActuel;
	uint8_t nbcallbacks;
	uint16_t id;

		while(1)
		{
			if(serial_rb.available())
			{
				unsigned char lecture[50];
				unsigned char entete;
				uint8_t index = 0;
				// V�rification de l'ent�te

//				sendPong();
//				askResend(0);
//				serial_rb.send_char(0x99);
				serial_rb.read_char(&entete);
//				serial_rb.send_char(entete);
				if(entete != 0x55)
				{
//				serial_rb.send_char(0x88);
					continue;
				}
//				serial_rb.send_char(0x10);

				serial_rb.read_char(&entete);
				if(entete != 0xAA)
				{
//					serial_rb.send_char(0x89);

					continue;
				}
//			   serial_rb.send_char(0x11);

//				serial_rb.send_char(0x10);
				// R�cup�ration de l'id
				serial_rb.read_char(lecture); // id point fort
//				serial_rb.send_char(0x11);
				serial_rb.read_char(lecture+(++index)); // id point faible
//				serial_rb.send_char(0x12);
				uint16_t idPaquet = (lecture[ID_FORT] << 8) + lecture[ID_FAIBLE];

				// On redemande les paquets manquants si besoin est
				if(idPaquet > idDernierPaquet)
				{
					idDernierPaquet++; // id paquet th�oriquement re�u
					while(idPaquet > idDernierPaquet)
						askResend(idDernierPaquet++);
				}

				// Si on re�oit un ID ancien� on fait comme si de rien n'�tait

				serial_rb.read_char(lecture+(++index)); // lecture de la commande

//				serial_rb.send_char(lecture[COMMANDE]);

				if(lecture[COMMANDE] == IN_PING_NEW_CONNECTION)
				{
					if(!verifieChecksum(lecture, index))
						askResend(idPaquet);
					else
					{
						idDernierPaquet = idPaquet; // on r�initialise le num�ro des paquets
						sendPong();
					}
				}
				else if(lecture[COMMANDE] == IN_PING)
				{
					serial_rb.read_char(lecture+(++index));
					// Cas particulier. Pas de checksum
					sendPong();
					if(ping == false)
					{
						ping = true;
//						HAL_GPIO_WritePin(GPIOC, GPIO_PIN_15, GPIO_PIN_SET); // on allume la led de ping C15
					}
				}
				else if(lecture[COMMANDE] == IN_DEBUG_MODE)
				{
					if(!verifieChecksum(lecture, index))
						askResend(idPaquet);
					else
						debugMode = true;
				}
				else if(lecture[COMMANDE] == IN_ACTIONNEURS)
				{
					serial_rb.read_char(lecture+(++index)); // id
					serial_rb.read_char(lecture+(++index)); // ordre
					serial_rb.read_char(lecture+(++index)); // ordre
					if(!verifieChecksum(lecture, index))
						askResend(idPaquet);
					else
						ax12[lecture[PARAM]]->goTo((lecture[PARAM + 1] << 8) + lecture[PARAM + 2]);
				}
				else if(lecture[COMMANDE] == IN_STOP)
				{
					if(!verifieChecksum(lecture, index))
						askResend(idPaquet);
					else
					{
						while(xSemaphoreTake(consigneAsser_mutex, (TickType_t) (ATTENTE_MUTEX_MS / portTICK_PERIOD_MS)) != pdTRUE);
						changeModeAsserActuel(STOP);
						xSemaphoreGive(consigneAsser_mutex);
					}
				}

				else if(lecture[COMMANDE] == IN_PAUSE_MOVE)
				{
					if(!verifieChecksum(lecture, index))
						askResend(idPaquet);
					else
						pauseAsser = true;
				}

				else if(lecture[COMMANDE] == IN_RESUME_MOVE)
				{
					if(!verifieChecksum(lecture, index))
						askResend(idPaquet);
					else
						pauseAsser = false;
				}

				else if(lecture[COMMANDE] == IN_ASSER_POS_ACTUELLE)
				{
					if(!verifieChecksum(lecture, index))
						askResend(idPaquet);
					else
					{
						while(xSemaphoreTake(consigneAsser_mutex, (TickType_t) (ATTENTE_MUTEX_MS / portTICK_PERIOD_MS)) != pdTRUE);
						changeModeAsserActuel(SUR_PLACE);
                        maxTranslationSpeed = 100;
                        maxRotationSpeed = 100;
						consigneX = x_odo;
						consigneY = y_odo;
						xSemaphoreGive(consigneAsser_mutex);
					}
				}

				else if(lecture[COMMANDE] == IN_TOURNER)
				{
					serial_rb.read_char(lecture+(++index)); // angle
					serial_rb.read_char(lecture+(++index)); // angle
					serial_rb.read_char(lecture+(++index)); // v
					serial_rb.read_char(lecture+(++index)); // v
					if(!verifieChecksum(lecture, index))
						askResend(idPaquet);
					else
					{
						float angle = ((lecture[PARAM] << 8) + lecture[PARAM + 1]) / 1000.;

						while(xSemaphoreTake(consigneAsser_mutex, (TickType_t) (ATTENTE_MUTEX_MS / portTICK_PERIOD_MS)) != pdTRUE);
						rotationSetpoint = RAD_TO_TICK(angle);
						changeModeAsserActuel(ROTATION);
						consigneX = x_odo;
						consigneY = y_odo;
                        maxTranslationSpeed = VITESSE_LINEAIRE_MAX;
                        maxRotationSpeed = ((lecture[PARAM + 2] << 8) + lecture[PARAM + 3]) *1. / 1000. / RAD_PAR_TICK / FREQUENCE_ODO_ASSER;
						xSemaphoreGive(consigneAsser_mutex);
					}
				}
/*
				else if(lecture[COMMANDE] == IN_VITESSE)
				{
					serial_rb.read_char(lecture+(++index));
					serial_rb.read_char(lecture+(++index));
					serial_rb.read_char(lecture+(++index));
					serial_rb.read_char(lecture+(++index));
					if(!verifieChecksum(lecture, index))
						askResend(idPaquet);
					else
					{
						asserVitesseGauche = (lecture[PARAM] << 8) + lecture[PARAM + 1];
						asserVitesseDroite	 = (lecture[PARAM + 2] << 8) + lecture[PARAM + 3];

						while(xSemaphoreTake(consigneAsser_mutex, (TickType_t) (ATTENTE_MUTEX_MS / portTICK_PERIOD_MS)) != pdTRUE);

						changeModeAsserActuel(ASSER_VITESSE);
						xSemaphoreGive(consigneAsser_mutex);
					}
				}
*/
				else if((lecture[COMMANDE] & IN_AVANCER_MASQUE) == IN_AVANCER)
				{
					serial_rb.read_char(lecture+(++index)); // d
					serial_rb.read_char(lecture+(++index)); // d
					serial_rb.read_char(lecture+(++index)); // v
					serial_rb.read_char(lecture+(++index)); // v
					if(!verifieChecksum(lecture, index))
						askResend(idPaquet);
					else
					{
						int16_t distance = (lecture[PARAM] << 8) + lecture[PARAM + 1];

						if(lecture[COMMANDE] == IN_AVANCER_NEG)
							distance = -distance;
						else if(lecture[COMMANDE] == IN_AVANCER_IDEM && !marcheAvant)
							distance = -distance;
						else if(lecture[COMMANDE] == IN_AVANCER_REVERSE && marcheAvant)
							distance = -distance;

						marcheAvant = distance >= 0;

						while(xSemaphoreTake(consigneAsser_mutex, (TickType_t) (ATTENTE_MUTEX_MS / portTICK_PERIOD_MS)) != pdTRUE);
						changeModeAsserActuel(VA_AU_POINT);
						consigneX = cos_orientation_odo * distance + x_odo;
						consigneY = sin_orientation_odo * distance + y_odo;
                        maxRotationSpeed = VITESSE_ROTATION_MAX;
                        maxTranslationSpeed = ((lecture[PARAM + 2] << 8) + lecture[PARAM + 3]) * 1. / MM_PAR_TICK / FREQUENCE_ODO_ASSER;
//                        maxTranslationSpeed = VITESSE_LINEAIRE_MAX; // TODO�: � retirer !
						xSemaphoreGive(consigneAsser_mutex);
					}
				}

				else if(lecture[COMMANDE] == IN_VA_POINT)
				{
					serial_rb.read_char(lecture+(++index)); // x
					serial_rb.read_char(lecture+(++index)); // xy
					serial_rb.read_char(lecture+(++index)); // y
                    serial_rb.read_char(lecture+(++index)); // v
                    serial_rb.read_char(lecture+(++index)); // v

					if(!verifieChecksum(lecture, index))
						askResend(idPaquet);
					else
					{
						int16_t x = (lecture[PARAM] << 4) + (lecture[PARAM + 1] >> 4);
						x -= 1500;
						int16_t y = ((lecture[PARAM + 1] & 0x0F) << 8) + lecture[PARAM + 2];

						marcheAvant = (x - x_odo) * cos_orientation_odo + (y - y_odo) * sin_orientation_odo > 0;

						while(xSemaphoreTake(consigneAsser_mutex, (TickType_t) (ATTENTE_MUTEX_MS / portTICK_PERIOD_MS)) != pdTRUE);
						changeModeAsserActuel(VA_AU_POINT);
						consigneX = x;
						consigneY = y;
                        maxRotationSpeed = VITESSE_ROTATION_MAX;
                        maxTranslationSpeed = ((lecture[PARAM + 3] << 8) + lecture[PARAM + 4]) * 1. / MM_PAR_TICK / FREQUENCE_ODO_ASSER;
						xSemaphoreGive(consigneAsser_mutex);
					}
				}
				else if((lecture[COMMANDE] & 0xFE) == IN_ARC)
				{
					serial_rb.read_char(lecture+(++index)); // x
					serial_rb.read_char(lecture+(++index)); // xy
					serial_rb.read_char(lecture+(++index)); // y
					serial_rb.read_char(lecture+(++index)); // orientation
					serial_rb.read_char(lecture+(++index)); // orientation
					serial_rb.read_char(lecture+(++index)); // courbure
					serial_rb.read_char(lecture+(++index)); // courbure
					serial_rb.read_char(lecture+(++index)); // vitesse
					serial_rb.read_char(lecture+(++index)); // vitesse

					if(!verifieChecksum(lecture, index))
						askResend(idPaquet);
					else
					{
						while(xSemaphoreTake(consigneAsser_mutex, (TickType_t) (ATTENTE_MUTEX_MS / portTICK_PERIOD_MS)) != pdTRUE);

						int16_t x = (lecture[PARAM] << 4) + (lecture[PARAM + 1] >> 4);
						x -= 1500;
						int16_t y = ((lecture[PARAM + 1] & 0x0F) << 8) + lecture[PARAM + 2];
						float angle = ((lecture[PARAM + 3] << 8) + lecture[PARAM + 4]) / 1000.;

						float courbure = ((lecture[PARAM + 5] << 8) + lecture[PARAM + 6]) / 1000.;
                        float vitesse = ((lecture[PARAM + 7] << 8) + lecture[PARAM + 8]) * 1. / MM_PAR_TICK / FREQUENCE_ODO_ASSER;
						trajectoire[indiceTrajectoireEcriture].x = x;
						trajectoire[indiceTrajectoireEcriture].y = y;
						trajectoire[indiceTrajectoireEcriture].courbure = courbure - 20;
						trajectoire[indiceTrajectoireEcriture].orientation = RAD_TO_TICK(angle);
						trajectoire[indiceTrajectoireEcriture].vitesse = vitesse;
						trajectoire[indiceTrajectoireEcriture].dir_x = 1000 * cos(angle);
						trajectoire[indiceTrajectoireEcriture].dir_y = 1000 * sin(angle);

						// TODO r�initialiser les indices indiceArretEcriture et indiceArretLecture

						// On conserve l'arc pr�c�dent en arc d'arr�t si c'est explicitement demand� et
						// si l'arc qu'on vient d'obtenir n'est pas le premier
						if(lecture[COMMANDE] != IN_ARC && modeAsserActuel == COURBE)
							indiceArretEcriture++;

						// Dans tous les cas, on s'arr�te au dernier arc re�u
						arcsArret[indiceArretEcriture] = &trajectoire[indiceTrajectoireEcriture];
						arcsArret[indiceArretEcriture]->indiceAssocie = indiceTrajectoireEcriture;
						indiceTrajectoireEcriture++;

						changeModeAsserActuel(COURBE);
						xSemaphoreGive(consigneAsser_mutex);
					}
				}
                else if((lecture[COMMANDE] & IN_PID_CONST_MASQUE) == IN_PID_CONST_VIT_GAUCHE)
				{
					serial_rb.read_char(lecture+(++index)); // kp
					serial_rb.read_char(lecture+(++index)); // kp
					serial_rb.read_char(lecture+(++index)); // kp
					serial_rb.read_char(lecture+(++index)); // ki
					serial_rb.read_char(lecture+(++index)); // ki
					serial_rb.read_char(lecture+(++index)); // ki
					serial_rb.read_char(lecture+(++index)); // kd
					serial_rb.read_char(lecture+(++index)); // kd
					serial_rb.read_char(lecture+(++index)); // kd
					if(!verifieChecksum(lecture, index))
						askResend(idPaquet);
					else
					{
						float kp = ((lecture[PARAM] << 16) + (lecture[PARAM + 1] << 8) + lecture[PARAM + 2])/1000.;
						float ki = ((lecture[PARAM + 3] << 16) + (lecture[PARAM + 4] << 8) + lecture[PARAM + 5])/1000./FREQUENCE_ODO_ASSER;
						float kd = ((lecture[PARAM + 6] << 16) + (lecture[PARAM + 7] << 8) + lecture[PARAM + 8])/1000.*FREQUENCE_ODO_ASSER;
						if(lecture[COMMANDE] == IN_PID_CONST_VIT_GAUCHE)
							leftSpeedPID.setTunings(kp, ki, kd);
						else if(lecture[COMMANDE] == IN_PID_CONST_VIT_DROITE)
							rightSpeedPID.setTunings(kp, ki, kd);
						else if(lecture[COMMANDE] == IN_PID_CONST_TRANSLATION)
							translationPID.setTunings(kp, ki, kd);
						else if(lecture[COMMANDE] == IN_PID_CONST_ROTATION)
							rotationPID.setTunings(kp, ki, kd);
						else if(lecture[COMMANDE] == IN_PID_CONST_COURBURE)
							courburePID.setTunings(kp/1000., ki/1000., kd/1000.);
						else if(lecture[COMMANDE] == IN_CONST_SAMSON)
						{
							k1 = kp;
							k2 = ki;
						}
					}
				}
                else if(lecture[COMMANDE] == IN_ASSER_OFF)
                {
					if(!verifieChecksum(lecture, index))
						askResend(idPaquet);
					else
						changeModeAsserActuel(ASSER_OFF);
                }
				else if(lecture[COMMANDE] == IN_INIT_ODO)
				{
					serial_rb.read_char(lecture+(++index)); // x
					serial_rb.read_char(lecture+(++index)); // xy
					serial_rb.read_char(lecture+(++index)); // y
					serial_rb.read_char(lecture+(++index)); // o
					serial_rb.read_char(lecture+(++index)); // o

					if(!verifieChecksum(lecture, index))
						askResend(idPaquet);
					else
					{
						int16_t x = (lecture[PARAM] << 4) + (lecture[PARAM + 1] >> 4);
						x -= 1500;
						uint16_t y = ((lecture[PARAM + 1] & 0x0F) << 8) + lecture[PARAM + 2];
						uint16_t o = (lecture[PARAM + 3] << 8) + lecture[PARAM + 4];

						if(!startOdo)
						{
							x_odo = x;
							y_odo = y;
							orientation_odo = o/1000.;

							// On l'asservit sur place
							changeModeAsserActuel(SUR_PLACE);
							consigneX = x;
							consigneY = y;
	                        maxTranslationSpeed = 100;
	                        maxRotationSpeed = 100;
		//					orientationTick = RAD_TO_TICK(parseInt(lecture, &(++index))/1000.);
	//						serial_rb.printfln("%d",(int)orientationTick);
	//						serial_rb.printfln("%d",(int)(TICK_TO_RAD(orientationTick)*1000));
							startOdo = true;
						}
					}
//					else
//						serial_rb.printfln("ERR_ODO",(int)orientationTick);
				}

				else if(lecture[COMMANDE] == IN_RESEND_PACKET)
				{
					serial_rb.read_char(lecture+(++index)); // id
					serial_rb.read_char(lecture+(++index)); // id
					if(!verifieChecksum(lecture, index))
						askResend(idPaquet);
					else
					{
						uint16_t id = (lecture[PARAM] << 8) + lecture[PARAM + 1];
						resend(id);
					}
				}
				else if(lecture[COMMANDE] == IN_REMOVE_ALL_HOOKS)
				{
					if(!verifieChecksum(lecture, index))
						askResend(idPaquet);
					else
						listeHooks.clear();
				}
				else if(lecture[COMMANDE] == IN_REMOVE_SOME_HOOKS)
				{
					serial_rb.read_char(lecture+(++index)); // nb
					uint8_t nbIds = lecture[PARAM];
					vector<Hook*>::iterator it = listeHooks.begin();

					for(uint8_t i = 0; i < nbIds; i++)
					{
						serial_rb.read_char(lecture+(++index)); // date
						uint16_t id = lecture[PARAM + 1 + i];

						for(uint8_t j = 0; j < listeHooks.size(); j++)
						{
							Hook* hook = listeHooks[j];
							if(hook->getId() == id)
							{
								vPortFree(hook);
								listeHooks[j] = listeHooks.back();
								listeHooks.pop_back();
								break; // l'id est unique
							}
						}

					}
					if(!verifieChecksum(lecture, index))
						askResend(idPaquet);
				}
				else if((lecture[COMMANDE] & IN_HOOK_MASK) == IN_HOOK_GROUP)
				{
					if(lecture[COMMANDE] == IN_HOOK_DATE)
					{
						serial_rb.read_char(lecture+(++index)); // date
						serial_rb.read_char(lecture+(++index)); // date
						serial_rb.read_char(lecture+(++index)); // date
						serial_rb.read_char(lecture+(++index)); // id
						serial_rb.read_char(lecture+(++index)); // nb_callback

						uint32_t date = (lecture[PARAM] << 16) + (lecture[PARAM + 1] << 8) + lecture[PARAM + 2];
						id = lecture[PARAM + 3];
						nbcallbacks = lecture[PARAM + 4];
						hookActuel = new(pvPortMalloc(sizeof(HookTemps))) HookTemps(id, nbcallbacks, date);
						listeHooks.push_back(hookActuel);
					}
					else if((lecture[COMMANDE] & 0xFE) == IN_HOOK_CONTACT)
					{
						serial_rb.read_char(lecture+(++index)); // nb_capt
						serial_rb.read_char(lecture+(++index)); // id
						serial_rb.read_char(lecture+(++index)); // nb_callback
						uint8_t nbContact = lecture[PARAM];
						bool unique = lecture[COMMANDE] == IN_HOOK_CONTACT_UNIQUE;
						id = lecture[PARAM + 1];
						nbcallbacks = lecture[PARAM + 2];
						hookActuel = new(pvPortMalloc(sizeof(HookContact))) HookContact(id, unique, nbcallbacks, nbContact);
						listeHooks.push_back(hookActuel);
					}
					else if(lecture[COMMANDE] == IN_HOOK_DEMI_PLAN)
					{
						serial_rb.read_char(lecture+(++index)); // x point
						serial_rb.read_char(lecture+(++index)); // xy point
						serial_rb.read_char(lecture+(++index)); // y point
						serial_rb.read_char(lecture+(++index)); // x direction
						serial_rb.read_char(lecture+(++index)); // xy direction
						serial_rb.read_char(lecture+(++index)); // y direction
						serial_rb.read_char(lecture+(++index)); // id
						serial_rb.read_char(lecture+(++index)); // nb_callback

						float x = (lecture[PARAM] << 4) + (lecture[PARAM + 1] >> 4);
						x -= 1500;
						float y = ((lecture[PARAM + 1] & 0x0F) << 8) + lecture[PARAM + 2];

						float dir_x = (lecture[PARAM + 3] << 4) + (lecture[PARAM + 4] >> 4);
						dir_x -= 1500;
						float dir_y = ((lecture[PARAM + 4] & 0x0F) << 8) + lecture[PARAM + 5];
						dir_y -= 1500;
						id = lecture[PARAM + 6];
						nbcallbacks = lecture[PARAM + 7];
						hookActuel = new(pvPortMalloc(sizeof(HookDemiPlan))) HookDemiPlan(id, nbcallbacks, x, y, dir_x, dir_y);
						listeHooks.push_back(hookActuel);
					}
					else if((lecture[COMMANDE] & 0xFE) == IN_HOOK_POSITION)
					{
						serial_rb.read_char(lecture+(++index)); // x
						serial_rb.read_char(lecture+(++index)); // xy
						serial_rb.read_char(lecture+(++index)); // y
						serial_rb.read_char(lecture+(++index)); // rayon
						serial_rb.read_char(lecture+(++index)); // rayon

						int16_t x = (lecture[PARAM] << 4) + (lecture[PARAM + 1] >> 4);
						x -= 1500;
						uint16_t y = ((lecture[PARAM + 1] & 0x0F) << 8) + lecture[PARAM + 2];

						uint32_t tolerance = (lecture[PARAM + 3] << 8) + lecture[PARAM + 4];
						id = lecture[PARAM + 5];
						nbcallbacks = lecture[PARAM + 6];
						hookActuel = new(pvPortMalloc(sizeof(HookPosition))) HookPosition(id, nbcallbacks, x, y, tolerance);
						listeHooks.push_back(hookActuel);
					}
					else
						continue;

					for(int i = 0; i < nbcallbacks; i++)
					{
						serial_rb.read_char(lecture+(++index)); // callback
						if(((lecture[index]) & IN_CALLBACK_MASK) == IN_CALLBACK_ELT)
						{
							uint8_t nbElem = lecture[index] & ~IN_CALLBACK_MASK;
							Exec_Update_Table* tmp = new(pvPortMalloc(sizeof(Exec_Update_Table))) Exec_Update_Table(nbElem);
							hookActuel->insert(tmp, i);
						}
						else if(((lecture[index]) & IN_CALLBACK_MASK) == IN_CALLBACK_SCRIPT)
						{
							uint8_t nbScript = lecture[index] & ~IN_CALLBACK_MASK;
							Exec_Script* tmp = new(pvPortMalloc(sizeof(Exec_Script))) Exec_Script(nbScript);
							hookActuel->insert(tmp, i);
						}
						else if(((lecture[index]) & IN_CALLBACK_MASK) == IN_CALLBACK_AX12)
						{
							uint8_t nbAct = lecture[index] & ~IN_CALLBACK_MASK;
							serial_rb.read_char(lecture+(++index));
							serial_rb.read_char(lecture+(++index));
							uint16_t angle = (lecture[index - 1] << 8) + lecture[index];
							Exec_Act* tmp = new(pvPortMalloc(sizeof(Exec_Act))) Exec_Act(ax12[nbAct], angle);
							hookActuel->insert(tmp, i);
						}
					}
				}
			}

				//					serial_rb.printfln("color rouge");

			else // on n'attend que s'il n'y avait rien. Ainsi, si la s�rie prend du retard elle n'attend pas pour traiter toutes les donn�es entrantes suivantes
			{
//				vTaskDelay(1);
			}
		}
//			serial_rb.printfln("%d", TIM5->CNT);

}

#endif
