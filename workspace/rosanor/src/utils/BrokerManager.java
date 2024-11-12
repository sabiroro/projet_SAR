package utils;

import java.util.HashMap;

import abstracts.Broker;

public class BrokerManager {
    // Singleton
    private static final BrokerManager instance;

    // Map enabling the association of the broker name with the broker object
    public final HashMap<String, Broker> brokers;

    // Runs on class loading from the JVM
    static {
        instance = new BrokerManager();
    }

    private BrokerManager() {
        this.brokers = new HashMap<>();
    }

    public static BrokerManager getInstance() {
        return instance;
    }

    public synchronized void addBroker(String name, Broker broker) {
        Broker existingBroker = brokers.get(name);
        if (existingBroker != null) {
            throw new IllegalArgumentException("Broker with name " + name + " already exists");
        }
        brokers.put(name, broker);
    }

    public synchronized Broker getBroker(String name) {
        return brokers.get(name);
    }

    public synchronized boolean brokerExists(String name) {
        return brokers.get(name) != null;
    }
    
    public synchronized Broker removeBroker(String name) {
        return brokers.remove(name);
    }

    public synchronized void removeAllBrokers() {
        brokers.clear();
    }
}
