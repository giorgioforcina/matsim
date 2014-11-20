/* *********************************************************************** *
 * project: kai
 * Main.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package tutorial.programming.ownMobsimAgentWithPerception;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.ControlerConfigGroup.MobsimType;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.MobsimRegistrar;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

/**
 * Untested code.  Idea is that an observer notes the traffic congestion, and returns the "best" of all outgoing links to the vehicle.
 * 
 * @author nagel
 */
class Main {

	public static void main(String[] args) {
		
		final Controler ctrl = new Controler( args[0] ) ;

		// observer.  Will probably NOT need one instance per agent in order to be thread safe since the threads will only get info from this but not set.
		// However, if one wants different perceptions per agent then one also needs different observers.  Or observers that are parameterized in the agents. 
		final MyObserver eventsObserver = new MyObserver( ctrl.getScenario() ) ;
		ctrl.getEvents().addHandler( eventsObserver );
		
		// guidance.  Will need one instance per agent in order to be thread safe
		final MyGuidance guidance = new MyGuidance( eventsObserver, ctrl.getScenario() ) ;
		
		ctrl.setMobsimFactory(new MobsimFactory(){
			@Override
			public Mobsim createMobsim(Scenario sc, EventsManager eventsManager) {
				
				MobsimFactory factory = new MobsimRegistrar().getFactoryRegister().getInstance( MobsimType.qsim.toString() ) ;
				// (this takes the default QSim factory from the MATSim platform.  One could as well just copy the constructor from there. kai, nov'14)

				final QSim qsim = (QSim) factory.createMobsim(sc, eventsManager) ;
				
				// Why agent source instead of inserting them directly?  Inserting agents into activities is, in fact possible just
				// after the QSim constructor.  However, inserting vehicles or agents into links is not.  Agentsource makes
				// sure that this is appropriately delayed.
				qsim.addAgentSource(new AgentSource(){
					@Override
					public void insertAgentsIntoMobsim() {
						// insert traveler agent:
						final MobsimAgent ag = new MyMobsimAgent( guidance ) ;
						qsim.insertAgentIntoMobsim(ag) ;
						
						// insert vehicle:
						final Vehicle vehicle = VehicleUtils.getFactory().createVehicle(Id.create(ag.getId(), Vehicle.class), VehicleUtils.getDefaultVehicleType() );
						Id<Link> linkId4VehicleInsertion = null ;
						qsim.createAndParkVehicleOnLink(vehicle, linkId4VehicleInsertion);
					}
				}) ;
				return qsim ;
			}
		}) ;
	}

}
