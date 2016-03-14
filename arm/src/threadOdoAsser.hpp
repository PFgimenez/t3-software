#ifndef TH_ODO_ASSER
#define TH_ODO_ASSER

#include <stdio.h>
#include <stdlib.h>
#include <vector>
#include "diag/Trace.h"

#include "math.h"
#include "Timer.h"
#include "FreeRTOS.h"
#include "task.h"
#include "Hook.h"
#include "Uart.hpp"
#include "global.h"
#include "ax12.hpp"
#include "serie.h"
#include "serialProtocol.h"
#include "asserSimple.hpp"

using namespace std;


/**
 * Thread d'odom�trie et d'asservissement
 */
void thread_odometrie_asser(void* p)
{
	int16_t positionGauche[MEMOIRE_VITESSE]; // on introduit un effet de m�moire afin de pouvoir mesurer la vitesse sur un intervalle pas trop petit
	int16_t positionDroite[MEMOIRE_VITESSE];
	uint8_t indiceMemoire = 0;
	x_odo = 0;
	y_odo = 0;
	orientation_odo = 0;
	courbure_odo = 0;
	vg_odo = 0;
	vd_odo = 0;
	orientationTick_odo = 0;
	uint32_t orientationMoyTick = 0;;
	uint16_t old_tick_gauche = TICK_CODEUR_GAUCHE, old_tick_droit = TICK_CODEUR_DROIT, tmp;
	int16_t distanceTick, delta_tick_droit, delta_tick_gauche, deltaOrientationTick;
	double k, distance, deltaOrientation;

	for(int i = 0; i < MEMOIRE_VITESSE; i++)
	{
		positionDroite[i] = old_tick_droit;
		positionGauche[i] = old_tick_gauche;
	}

	// On attend l'initialisation de xyo avant de d�marrer l'odo, sinon �a casse tout.
	while(!startOdo)
		vTaskDelay(5);
	orientationTick_odo = RAD_TO_TICK(orientation_odo);
	while(1)
	{
		// ODOM�TRIE
		while(xSemaphoreTake(odo_mutex, (TickType_t) (ATTENTE_MUTEX_MS / portTICK_PERIOD_MS)) != pdTRUE);

		// La formule d'odom�trie est corrig�e pour tenir compte des trajectoires
		// (au lieu d'avoir une approximation lin�aire, on a une approximation circulaire)
		tmp = TICK_CODEUR_GAUCHE;
		delta_tick_gauche = tmp - old_tick_gauche;
		old_tick_gauche = tmp;
		vg_odo = (tmp - positionGauche[indiceMemoire]) / MEMOIRE_VITESSE;
		positionGauche[indiceMemoire] = tmp;

		tmp = TICK_CODEUR_DROIT;
		delta_tick_droit = tmp - old_tick_droit;
		old_tick_droit = tmp;
		vd_odo = (tmp - positionDroite[indiceMemoire]) / MEMOIRE_VITESSE;
		positionDroite[indiceMemoire] = tmp;

		vl_odo = (vd_odo + vg_odo) / 2;

		// Calcul issu de Thal�s. Position si le robot tourne vers la droite (pour �tre coh�rent avec l'orientation)
		courbure_odo = 2 / LONGUEUR_CODEUSE_A_CODEUSE_EN_MM * (vg_odo - vd_odo) / (vg_odo + vd_odo);

		indiceMemoire++;
		indiceMemoire %= MEMOIRE_VITESSE;

		// on �vite les formules avec "/ 2", qui font perdre de l'information et qui peuvent s'accumuler

		distanceTick = delta_tick_droit + delta_tick_gauche;
		distance = TICK_TO_MM(distanceTick);

		// gestion de la sym�trie
		if(!isSymmetry)
			deltaOrientationTick = delta_tick_droit - delta_tick_gauche;
		else
			deltaOrientationTick = delta_tick_gauche - delta_tick_droit;

		// l'erreur � cause du "/2" ne s'accumule pas
		orientationMoyTick = orientationTick_odo + deltaOrientationTick/2;

		if(orientationMoyTick > (uint32_t)TICKS_PAR_TOUR_ROBOT)
		{
			if(orientationMoyTick < (uint32_t)FRONTIERE_MODULO)
				orientationMoyTick -= (uint32_t)TICKS_PAR_TOUR_ROBOT;
			else
				orientationMoyTick += (uint32_t)TICKS_PAR_TOUR_ROBOT;
		}
		orientationTick_odo += deltaOrientationTick;
		deltaOrientation = TICK_TO_RAD(deltaOrientationTick);

//		serial_rb.printfln("TICKS_PAR_TOUR_ROBOT = %d", (int)TICKS_PAR_TOUR_ROBOT);
//		serial_rb.printfln("orientationMoyTick = %d", orientationMoyTick);
//		serial_rb.printfln("orientation = %d", (int)(orientation_odo*1000));

		if(deltaOrientationTick == 0) // afin d'�viter la division par 0
			k = 1.;
		else
			k = sin(deltaOrientation/2)/(deltaOrientation/2);

		if(distance == 0) //  �a va arriver quand on fait par exemple une rotation sur place.
			courbure_odo = 0;
		else
			courbure_odo = deltaOrientationTick / distance;

		orientation_odo = TICK_TO_RAD(orientationMoyTick);
        cos_orientation_odo = cos(orientation_odo);
        sin_orientation_odo = sin(orientation_odo);

		x_odo += k*distance*cos_orientation_odo;
		y_odo += k*distance*sin_orientation_odo;
		xSemaphoreGive(odo_mutex);

		//�ASSERVISSEMENT
		if(modeAsserActuel == PAS_BOUGER)
			controlRotation();
		else
        {
            if(modeAsserActuel == ROTATION)
            {
            	updateErrorAngle();
			controlRotation();
            }
		    else if(modeAsserActuel == STOP)
			    controlStop();
//		    else if(modeAsserActuel == TRANSLATION)
//			    controlTranslation();
		    else if(modeAsserActuel == COURBE)
			    controlTrajectoire();
            if(checkArrivee()) // gestion de la fin du mouvement
            {
                modeAsserActuel = PAS_BOUGER;
                sendArrive();
            }
        }
		// si �a vaut ASSER_OFF, il n'y a pas d'asser

//		vTaskDelay(1000);
		vTaskDelay(1000 / FREQUENCE_ODO_ASSER);
	}
}

#endif
