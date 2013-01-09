/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.andreas.P2.replanning;

import java.io.BufferedWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.io.IOUtils;

import playground.andreas.P2.helper.PConfigGroup;
import playground.andreas.P2.helper.PConstants;

/**
 * Will consider all activities up to 30 o'clock. All remaining activities are dropped.
 * 
 * @author aneumann
 *
 */
public class TimeProvider implements ActivityStartEventHandler, ActivityEndEventHandler{
	
	private final static Logger log = Logger.getLogger(TimeProvider.class);
	private final double maxTime = 30.0 * 3600.0;
	
	private final double timeSlotSize;
	private int[] currentWeights = null;
	private int[] tempWeights = null;
	private BufferedWriter writer = null;
	
	public TimeProvider(PConfigGroup pConfig, String outputDir){
		this.timeSlotSize = pConfig.getTimeSlotSize();
		
		int numberOfSlots = TimeProvider.getSlotForTime(this.maxTime, this.timeSlotSize);
		if (numberOfSlots == 0) {
			log.warn("Calculated number of slots is zero. MaxTime: " + this.maxTime + ", timeSlotSize: " + this.timeSlotSize);
			numberOfSlots = 1;
			log.warn("Number of slots is increased to " + numberOfSlots);
		}
		this.currentWeights = new int[numberOfSlots];
		this.tempWeights = new int[numberOfSlots];
		
		this.writer = IOUtils.getBufferedWriter(outputDir + PConstants.statsOutputFolder + "timeSlots2weight.txt");
		StringBuffer strB = new StringBuffer();
		
		for (int i = 0; i < currentWeights.length; i++) {
			strB.append("; " + i * timeSlotSize);
		}
		
		try {
			this.writer.write("# iteration" + strB.toString()); this.writer.newLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}
	
	@Override
	public void reset(int iteration) {
		// New Iteration - write the old weights to file and set the new ones as current
		this.writeToFile(this.writer, this.currentWeights, iteration);
		this.currentWeights = this.tempWeights;
		this.tempWeights = new int[this.currentWeights.length];
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		// Any activity will be tracked
		this.addOneToTimeSlot(event.getTime());
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		// Any activity will be tracked
		this.addOneToTimeSlot(event.getTime());
	}

	public double getRandomTimeInInterval(double startTime, double endTime) {
		int startSlot = TimeProvider.getSlotForTime(startTime, this.timeSlotSize);
		int endSlot = TimeProvider.getSlotForTime(endTime, this.timeSlotSize);
		
		if (startSlot > this.currentWeights.length || endSlot > this.currentWeights.length) {
			
		}
		
		int numberOfValidSlots = endSlot - startSlot + 1;
		
		// get total weight of all valid time slots
		int totalWeight = 0;
		for (int i = startSlot; i <= endSlot; i++) {
			totalWeight += this.currentWeights[i];
		}
		
		if (totalWeight == 0.0) {
			log.info("Total weight is zero. Probably first iteration. Will pick time slots randomly.");
			int numberOfRemainingSlots = numberOfValidSlots;
			for (int i = startSlot; i <= endSlot; i++) {
				if(MatsimRandom.getRandom().nextDouble() < 1.0 / numberOfRemainingSlots){
					return i * this.timeSlotSize;
				}
				numberOfRemainingSlots--;
			}
		} else {
			double rnd = MatsimRandom.getRandom().nextDouble() * totalWeight;
			double accumulatedWeight = 0.0;
			for (int i = startSlot; i <= endSlot; i++) {
				accumulatedWeight += this.currentWeights[i];
				if (accumulatedWeight >= rnd) {
					return i * this.timeSlotSize;
				}
			}
		}
		
		log.warn("Could not find any time slot. This should not happen. Check time slot size in config. Will return the start slot time");
		return startSlot * this.timeSlotSize;
	}
	
	private void writeToFile(BufferedWriter writer, int[] weights, int currentIteration) {
		StringBuffer strB = new StringBuffer();
		strB.append(currentIteration);
		
		for (int i = 0; i < weights.length; i++) {
			strB.append("; " + weights[i]);
		}
		
		try {
			writer.write(strB.toString()); writer.newLine();
			writer.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void addOneToTimeSlot(double time) {
		if(time <= this.maxTime) {
			int timeSlot = getSlotForTime(time, this.timeSlotSize);
			this.tempWeights[timeSlot]++;
		}
	}

	public static int getSlotForTime(double time, double timeSlotSize){
		return (int) (time / timeSlotSize);
	}
}
