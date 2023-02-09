package edu.eci.arsw.highlandersim;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class Immortal extends Thread {

    private ImmortalUpdateReportCallback updateCallback=null;
    
    private int health;
    
    private int defaultDamageValue;

    private final List<Immortal> immortalsPopulation;

    private final String name;

    private final Random r = new Random(System.currentTimeMillis());
    private final AtomicBoolean lockJefe;
    public static final AtomicInteger lockHilos = new AtomicInteger(0);


    public Immortal(String name, List<Immortal> immortalsPopulation, int health, int defaultDamageValue, ImmortalUpdateReportCallback ucb, AtomicBoolean lockJefe) {
        super(name);
        this.updateCallback=ucb;
        this.name = name;
        this.immortalsPopulation = immortalsPopulation;
        this.health = health;
        this.defaultDamageValue=defaultDamageValue;
        this.lockJefe = lockJefe;
    }

    @Override
    public void run() {
        while (true) {
            checkPause();
            Immortal im;
            int myIndex = immortalsPopulation.indexOf(this);
            int nextFighterIndex = r.nextInt(immortalsPopulation.size());
            //avoid self-fight
            if (nextFighterIndex == myIndex) {
                nextFighterIndex = ((nextFighterIndex + 1) % immortalsPopulation.size());
            }
            im = immortalsPopulation.get(nextFighterIndex);
            fightInOrder(im);
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    private void fightInOrder(Immortal im){
        int fromHash = System.identityHashCode(this);
        int toHash = System.identityHashCode(im);
        Immortal smallerHash = fromHash > toHash ? im : this;
        Immortal biggerHash = fromHash > toHash ? this : im;
        synchronized (smallerHash){
            synchronized (biggerHash){
                this.fight(im);
            }
        }
    }

    private void checkPause(){
        if(lockJefe.get()){
            try {
                lockHilos.addAndGet(1);
                if(lockHilos.get() == immortalsPopulation.size()) {
                    synchronized (lockJefe){
                        lockJefe.notifyAll();
                    }
                    lockHilos.set(0);
                }
                synchronized (lockHilos){
                    lockHilos.wait();
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void fight(Immortal i2) {
        if (i2.getHealth() > 0) {
            i2.changeHealth(i2.getHealth() - defaultDamageValue);
            this.health += defaultDamageValue;
            updateCallback.processReport("Fight: " + this + " vs " + i2+"\n");
        } else {
            updateCallback.processReport(this + " says:" + i2 + " is already dead!\n");
        }
    }

    public void changeHealth(int v) {
        health = v;
    }

    public int getHealth() {
        return health;
    }

    @Override
    public String toString() {

        return name + "[" + health + "]";
    }

}
