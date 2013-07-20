/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.bigpoint.assessment.gasstation.implementation;

import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.bigpoint.assessment.gasstation.GasPump;
import net.bigpoint.assessment.gasstation.GasType;
import net.bigpoint.assessment.gasstation.exceptions.GasTooExpensiveException;
import net.bigpoint.assessment.gasstation.exceptions.NotEnoughGasException;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

/**
 * Test for Station class.
 * @author gianksp
 */
public class StationTest {

    private static final Logger LOG = Logger.getLogger(StationTest.class.getName());
    private Station station;
    private final double dieselPrice = 0.76;
    private final double regularPrice = 0.56;
    private final double superPrice = 0.96;
    private final double defaultLiters = 5000;
    private final double minLiters = 5;

    /**
     * Test Station Configuration and Layout
     * - [Diesel pumps]
     *      quantity: 2 
     *      individual capacity: 5 liters 
     *      total capacity: 10 liters 
     *      price per liter: $0.76
     * - [Regular pumps]
     *      quantity: 3
     *      individual capacity: 5000 liters 
     *      total capacity: 150000 liters 
     *      price per liter: $0.56
     */
    @BeforeTest
    public void setUp() {
        //Create station
        station = new Station();
        //Set prices
        station.setPrice(GasType.REGULAR, regularPrice);
        station.setPrice(GasType.SUPER, superPrice);
        station.setPrice(GasType.DIESEL, dieselPrice);
        //Add pumps
        GasPump pumpDiesel1 = new GasPump(GasType.DIESEL, minLiters);
        GasPump pumpDiesel2 = new GasPump(GasType.DIESEL, minLiters);
        //Create regular pumps
        GasPump pumpRegular1 = new GasPump(GasType.REGULAR, defaultLiters);
        GasPump pumpRegular2 = new GasPump(GasType.REGULAR, defaultLiters);
        GasPump pumpRegular3 = new GasPump(GasType.REGULAR, defaultLiters);
        //Add to station
        station.addGasPump(pumpDiesel1);
        station.addGasPump(pumpRegular1);
        station.addGasPump(pumpRegular3);
        station.addGasPump(pumpRegular2);
        station.addGasPump(pumpDiesel2);
        LOG.info("Station created successfully");
    }

    /**
     * Test of setPrice method, of class Station.
     */
    @Test(threadPoolSize = 1, invocationCount = 1)
    public void testPrice() {
        long idThread = Thread.currentThread().getId();
        LOG.info("[testSetPrice] Setting prices for gas types. Current Thread: "+idThread);
        Random random = new Random();
        double rPrice = random.nextDouble();
        double sPrice = random.nextDouble();
        double dPrice = random.nextDouble();
        station.setPrice(GasType.REGULAR, rPrice);
        station.setPrice(GasType.SUPER, sPrice);
        station.setPrice(GasType.DIESEL, dPrice);
        Assert.assertTrue(  station.getPrice(GasType.REGULAR) == rPrice &&
                            station.getPrice(GasType.SUPER) == sPrice &&
                            station.getPrice(GasType.DIESEL) == dPrice);
    }

    /**
     * Test of addGasPump method, of class Station. 
     */
    @Test(threadPoolSize = 1, invocationCount = 1)
    public void testGasPumps() {
        long idThread = Thread.currentThread().getId();
        LOG.info("[testGasPumps] Adding gas pumps. Current Thread: "+idThread);
        GasPump pumpDiesel1 = new GasPump(GasType.DIESEL, minLiters);
        GasPump pumpRegular1 = new GasPump(GasType.REGULAR, defaultLiters);
        GasPump pumpSuper1 = new GasPump(GasType.SUPER, defaultLiters);
        station.addGasPump(pumpDiesel1);
        station.addGasPump(pumpRegular1);
        station.addGasPump(pumpSuper1);
        //Take into consideration the 3 added here and the 5 added default
        Assert.assertTrue(station.getGasPumps().size() == 8);
    }

    /**
     * Test of buyGas method, of class Station. We invoke this test from 25 different
     * threads simultaneously.
     */
    @Test(threadPoolSize = 25, invocationCount = 25, dependsOnMethods = {"testGasPumps"})
    public void testBuyGas() throws Exception {
        long idThread = Thread.currentThread().getId();
        LOG.info("[testBuyGas] Buying gas. Current Thread: "+idThread);
        double pricePerLiter = station.getPrice(GasType.SUPER);
        Random random = new Random();
        double liters = random.nextDouble()*10;
        double price = 0;
        double estimatedPrice = liters * pricePerLiter;
        try {
            price = station.buyGas(GasType.SUPER, liters, 1);
            LOG.info("[testBuyGas] Price paid:"+price+". Current Thread: "+idThread);
        } catch (NotEnoughGasException ex) {
            LOG.log(Level.SEVERE,"Not enough gas");
        } catch (GasTooExpensiveException ex) {
            LOG.log(Level.SEVERE,"Gas too expensive");
        }  
        Assert.assertTrue(price == estimatedPrice);
    }
    
    /**
     * Force the use of multiple gas pumps of the same type. If for a transaction requested
     * the first found pump of the given type has no capacity to serve (not enough gas), check
     * if there are more pumps available with that gas type that can within the station.
     */
    @Test(threadPoolSize = 15, invocationCount = 15, dependsOnMethods = {"testGasPumps"})
    public void testBuyGasDepleted() throws Exception {
        long idThread = Thread.currentThread().getId();
        boolean success = false;
        LOG.info("[testBuyGasDepleted] Buying gas. Current Thread: "+idThread);
        try {
            station.buyGas(GasType.DIESEL, 1, 1);
            success = true;
        } catch (NotEnoughGasException ex) {
            LOG.log(Level.SEVERE,"Not enough gas");
        } catch (GasTooExpensiveException ex) {
            LOG.log(Level.SEVERE,"Gas too expensive");
        }  
        Assert.assertTrue(success);
    }
    
    /**
     * Test the NoGas Exception. We will request an amount of fuel that no configured pump
     * has in order to force it.
     * @throws Exception 
     */
    @Test(threadPoolSize = 1, invocationCount = 1, dependsOnMethods = {"testGasPumps"})
    public void testNoGasException() throws Exception {
        long idThread = Thread.currentThread().getId();
        boolean success = false;
        LOG.info("[testNoGasException] Buying gas. Current Thread: "+idThread);
        try {
            station.buyGas(GasType.DIESEL, 100000, 1);
        } catch (NotEnoughGasException ex) {
            success = true;
            LOG.log(Level.SEVERE,"Not enough gas");
        } catch (GasTooExpensiveException ex) {
            LOG.log(Level.SEVERE,"Gas too expensive");
        }  
        Assert.assertTrue(success);
    }
    
    /**
     * Test for the TooExpensive exception. We will set a max price to pay at zero indicating
     * we want the fuel for free, how cool would that be!
     * @throws Exception 
     */
    @Test(threadPoolSize = 1, invocationCount = 1, dependsOnMethods = {"testGasPumps"})
    public void testTooExpensiveException() throws Exception {
        long idThread = Thread.currentThread().getId();
        boolean success = false;
        LOG.info("[testTooExpensiveException] Buying gas. Current Thread: "+idThread);
        try {
            station.buyGas(GasType.REGULAR, 1, 0);
        } catch (NotEnoughGasException ex) {
            LOG.log(Level.SEVERE,"Not enough gas");
        } catch (GasTooExpensiveException ex) {
            success = true;
            LOG.log(Level.SEVERE,"Gas too expensive");
        }  
        Assert.assertTrue(success);
    }
}
