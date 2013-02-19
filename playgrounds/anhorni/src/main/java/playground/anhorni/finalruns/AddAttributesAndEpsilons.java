package playground.anhorni.finalruns;

import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.facilities.FacilitiesReaderMatsimV1;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import playground.anhorni.csestimation.Location;
import playground.anhorni.csestimation.ShopLocation;
import playground.anhorni.csestimation.ShopsEnricher;
import playground.anhorni.csestimation.UniversalChoiceSetReader;
import playground.anhorni.csestimation.Utils;

public class AddAttributesAndEpsilons {	
	private double avg_size = 0.0;
	private double avg_price = 0.0;
	
	private ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
	private final static Logger log = Logger.getLogger(AddAttributesAndEpsilons.class);
	
	private String outdir;
	
	public static void main(final String[] args) {		
		AddAttributesAndEpsilons adapter = new AddAttributesAndEpsilons();		
		adapter.run(args[0], args[1], args[2], args[3], args[4], args[5]);		
		log.info("Adaptation finished -----------------------------------------");
	}
	
	public void run(final String plansFilePath, 
			final String networkFilePath, 
			final String facilitiesFilePath, 
			final String outputFolder, 
			final String bzFile,
			final String csFile) {
		this.init(plansFilePath, networkFilePath, facilitiesFilePath, outputFolder);
		this.assignSizeAndPrice(bzFile, csFile);
	}
	
	private void init(final String plansFilePath, final String networkFilePath, final String facilitiesFilePath, final String outputFolder) {		
		this.outdir = outputFolder;
		new MatsimNetworkReader(scenario).readFile(networkFilePath);		
		new FacilitiesReaderMatsimV1(scenario).readFile(facilitiesFilePath);
		MatsimPopulationReader populationReader = new MatsimPopulationReader(this.scenario);
		populationReader.readFile(plansFilePath);
	}
	
	private void assignSizeAndPrice(final String bzFile, final String csFile) {
		TreeMap<Id, ShopLocation> shops = new TreeMap<Id, ShopLocation>();		
		
		UniversalChoiceSetReader ucsReader = new UniversalChoiceSetReader();
		TreeMap<Id, ShopLocation> shopsCS = ucsReader.readUniversalCS(csFile);
		
		ShopsEnricher enricher = new ShopsEnricher();
		enricher.enrich(shopsCS, bzFile);
		
		QuadTree<Location> shopsCSQuadTree = Utils.buildLocationQuadTree(shopsCS); 
		
		for (ActivityFacility f:this.scenario.getActivityFacilities().getFacilities().values()) {
			ShopLocation shop = new ShopLocation(f.getId(), f.getCoord());
			shops.put(f.getId(), shop);
			
			ShopLocation closestShopCS = (ShopLocation) shopsCSQuadTree.get(shop.getCoord().getX(), shop.getCoord().getY());
			
			if (CoordUtils.calcDistance(closestShopCS.getCoord(), shop.getCoord()) < 200) {
				shop.setPrice(closestShopCS.getPrice());
			}
			
			/*
			B015212A	0	Warenhäuser
			B015211A	1	Verbrauchermärkte (> 2500 m2)
			B015211B	2	Grosse Supermärkte (1000-2499 m2)
			B015211C	3	Kleine Supermärkte (400-999 m2)
			B015211D	4	Grosse Geschäfte (100-399 m2)
			B015211E	5	Kleine Geschäfte (< 100 m2)
			*/
						
			String keys [] = {"B015212A", "B015211A", "B015211B", "B015211C", "B015211D", "B015211E"};
			if (f.getActivityOptions().containsKey(keys[0])) {
				shop.setSize(0);
			} 
			else if (f.getActivityOptions().containsKey(keys[1])) {
				shop.setSize(1);
			}
			else if (f.getActivityOptions().containsKey(keys[2])) {
				shop.setSize(2);
			}
			else if (f.getActivityOptions().containsKey(keys[3])) {
				shop.setSize(3);
			}
			else if (f.getActivityOptions().containsKey(keys[4])) {
				shop.setSize(4);
			}
			else if (f.getActivityOptions().containsKey(keys[5])) {
				shop.setSize(5);
			}
			
		}			
		this.computeAttributeAverages(shops);
		this.writeScaledValues(shops);
	}
	
	private void computeAttributeAverages(TreeMap<Id, ShopLocation> shops) {
		int cntSize = 0;
		int cntPrice = 0;
		for (ShopLocation shop:shops.values()) {
			// write size and price
			if (shop.getPrice() > 0) {
				cntPrice++;
				this.avg_price += shop.getPrice();
			}
			if (shop.getSize() > 0) {
				cntSize++;
				this.avg_size += shop.getSize();
			}
		}
		this.avg_price /= cntPrice;
		this.avg_size /= cntSize;
	}
	
	private void writeScaledValues(TreeMap<Id, ShopLocation> shops) {
		ObjectAttributes facilitiyAttributes = new ObjectAttributes();	
		for (ShopLocation shop:shops.values()) {
			double priceScaled = this.avg_price;
			double sizeScaled = this.avg_size;
			if (shop.getPrice() > 0) {
				priceScaled = shop.getPrice() - this.avg_price;
			}
			else {
				priceScaled = this.avg_price;
			}
			if (shop.getSize() > 0) {
				sizeScaled = shop.getSize() - this.avg_size;
			}
			else {
				sizeScaled = this.avg_size;
			}
			facilitiyAttributes.putAttribute(shop.getId().toString(), "sizeScaled", sizeScaled);
			facilitiyAttributes.putAttribute(shop.getId().toString(), "priceScaled", priceScaled);
		}
		ObjectAttributesXmlWriter attributesWriter = new ObjectAttributesXmlWriter(facilitiyAttributes);
		attributesWriter.writeFile(this.outdir + "/facilityAttributes.xml");
		
		log.info("average size: " + this.avg_size);
		log.info("average price: " + this.avg_price);		
	}	
}
