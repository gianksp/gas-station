package net.bigpoint.assessment.gasstation.implementation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;
import net.bigpoint.assessment.gasstation.GasPump;
import net.bigpoint.assessment.gasstation.GasStation;
import net.bigpoint.assessment.gasstation.GasType;
import net.bigpoint.assessment.gasstation.exceptions.GasTooExpensiveException;
import net.bigpoint.assessment.gasstation.exceptions.NotEnoughGasException;

/**
 * Gas station facility class, it includes multiple gas pumps for different types of fuel
 * at different prices.
 * @author gianksp
 */
public class Station implements GasStation{
    
    /**
     * Logger instance. For assessment purposes logging will be extensively used.
     */
    private static final Logger LOG = Logger.getLogger(Station.class.getName());
    
    /**
     * Collection of Gas Pumps this station has.
     */
    private Collection<GasPump> pumps = new ArrayList<GasPump>();
    
    /**
     * Map with list of gas types and corresponding prices.
     */
    private Map<GasType, Double> prices = new HashMap<GasType, Double>();
    
    /**
     * Total revenue of the station.
     */
    private double revenue = 0;
    
    /**
     * Total sales of the station.
     */
    private int salesNumber = 0;
    
    /**
     * Total transactions canceled because of no gas.
     */
    private int cancellationNoGas = 0;
    
    /**
     * Total transactions canceled because of too expensive gas.
     */
    private int cancellationTooExpensive = 0;

    /**
     * Get collection of Gas Pumps this station has.
     * @return Collection of GasPumps
     */
    public Collection<GasPump> getGasPumps() {
        return this.pumps;
    }
        
    /**
     * Add a new GasPump to the collection of pumps this station has.
     * @param pump GasPump item
     */
    public void addGasPump(GasPump pump) {
        this.pumps.add(pump);
    }

    /**
     * Get total revenue of this station.
     * @return total revenue
     */
    public double getRevenue() {
        return this.revenue;
    }

    /**
     * Get total amount of sales performed by the station.
     * @return total sales
     */
    public int getNumberOfSales() {
        return this.salesNumber;
    }

    /**
     * Get total amount of operations canceled because of no gas.
     * @return total canceled because of no gas
     */
    public int getNumberOfCancellationsNoGas() {
        return this.cancellationNoGas;
    }

    /**
     * Get total amount of operations canceled because of too expensive
     * @return total canceled because of too expensive
     */
    public int getNumberOfCancellationsTooExpensive() {
        return this.cancellationTooExpensive;
    }
    
    /**
     * Get price for a given gas type.
     * @param type GasType
     * @return price
     */
    public double getPrice(GasType type) {
        return this.prices.get(type);
    }

    /**
     * Set price for a given gas type
     * @param type  GasType
     * @param price Price for gas type
     */
    public void setPrice(GasType type, double price) {
        this.prices.put(type, price);
    }
    
    /**
     * Let a customer buy gas by type, specifying total amount of liters needed and max price per liter to pay.
     * @param type              GasType
     * @param amountInLiters    Total liters needed
     * @param maxPricePerLiter  Max price willing to pay per liter
     * @return total price for transaction
     * @throws NotEnoughGasException
     * @throws GasTooExpensiveException 
     */
    public double buyGas(GasType type, double amountInLiters, double maxPricePerLiter) throws NotEnoughGasException, GasTooExpensiveException {

            double pricePerLiter = prices.get(type);
            double price = 0;

            //Validate price per liter. If the current price is higher than maxPricePerLiter param, throw exception
            if (pricePerLiter > maxPricePerLiter) {
                cancellationTooExpensive++;
                throw new GasTooExpensiveException();
            }

            //Iterate through pumps
            for (GasPump pump : pumps){
                //This pump serves the gas type requested
                if (pump.getGasType().equals(type)) {
                    //Lock it while using so no other Thread has access
                    synchronized(pump){
                        //This pump has enough fuel to serve
                        if (pump.getRemainingAmount() >= amountInLiters) {
                                pump.pumpGas(amountInLiters);
                                price = amountInLiters * pricePerLiter;
                                LOG.info("[PUMP STATISTICS] amount remaining: "+pump.getRemainingAmount());
                                revenue += price;
                                salesNumber++;
                                break;
                        }
                    }
                }
            }

            //We finalized iterating through every available pump and we know the client has enough money
            //If by the time we get here price is still 0 means no pump was available to attend it
            //either because it/them did not have enough fuel or that there is no pump for that
            //kind of fuel within the station, either way, throw NotEnoughGasException for the case.
            //The GasStation interface should include more exceptions for this method
            if (price == 0 && amountInLiters > 0){
                cancellationNoGas++;
                throw new NotEnoughGasException();        
            }

            return price;
    }
    
}