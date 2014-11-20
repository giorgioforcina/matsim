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

package tutorial.programming.ownMobsimAgentUsingRouter;

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
import org.matsim.core.router.TripRouter;
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
		
		// router.  In order to be thread safe, one needs one router per agent.  Since, on the other hand, routers are heavy-weight objects, 
		// this will not scale.  For large numbers of replanning agents, one needs to think of a better software architecture here. kai, nov'14
		TripRouter router = ctrl.getTripRouterFactory().instantiateAndConfigureTripRouter() ;
		
		// guidance.  Will need one instance per agent in order to be thread safe
		final MyGuidance guidance = new MyGuidance( router ) ;
		
		ctrl.setMobsimFactory(new MobsimFactory(){
			@Override
			public Mobsim createMobsim(final Scenario sc, EventsManager eventsManager) {
				
				MobsimFactory factory = new MobsimRegistrar().getFactoryRegister().getInstance( MobsimType.qsim.toString() ) ;
				// (this takes the default QSim factory from the MATSim platform.  One could as well just copy the constructor from there. kai, nov'14)
				
				final QSim qsim = (QSim) factory.createMobsim(sc, eventsManager) ;
				
				// Why AgentSource instead of inserting agents directly?  Inserting agents into activities is, in fact possible just
				// after the QSim constructor.  However, inserting vehicles or agents into links is not.  Agentsource makes
				// sure that this is appropriately delayed.
				qsim.addAgentSource(new AgentSource(){
					@Override
					public void insertAgentsIntoMobsim() {
						// insert traveler agent:
						final MobsimAgent ag = new MyMobsimAgent( guidance, qsim.getSimTimer(), sc ) ;
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
