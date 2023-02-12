package edu.eci.arsw.highlandersim;

import com.sun.org.apache.xpath.internal.operations.Bool;

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
    public static final AtomicInteger threadDead = new AtomicInteger(0);
    private AtomicBoolean dead;
    public static boolean allDead = false;


    public Immortal(String name, List<Immortal> immortalsPopulation, int health, int defaultDamageValue, ImmortalUpdateReportCallback ucb, AtomicBoolean lockJefe) {
        super(name);
        this.updateCallback=ucb;
        this.name = name;
        this.immortalsPopulation = immortalsPopulation;
        this.health = health;
        this.defaultDamageValue=defaultDamageValue;
        this.lockJefe = lockJefe;
        this.dead = new AtomicBoolean(false);
    }

    @Override
    public void run() {
        while (!dead.get() && (threadDead.get() + 1 != immortalsPopulation.size())) {
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
        synchronized (smallerHash) {
            synchronized (biggerHash) {
                if (!im.isDead() && !dead.get()) {
                    this.fight(im);
                }
            }
        }
    }

    private void checkPause(){
        if(lockJefe.get()){
            try {
                lockHilos.addAndGet(1);
                if(lockHilos.get() == (immortalsPopulation.size() - threadDead.get())) {
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
            if(i2.getHealth() == 0) {
                i2.stopImmortal();
            }
            updateCallback.processReport("Fight: " + this + " vs " + i2+"\n");
            if (threadDead.get() + 1  == immortalsPopulation.size()) {
                updateCallback.processReport("Winner : " + this.toString() + "\n");
                allDead = true;
            }
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
     public void stopImmortal() {
        dead.set(true);
        threadDead.addAndGet(1);
     }
    public boolean isDead() {
        return dead.get();
    }
}
