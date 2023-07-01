// Author:      Eddie F. Carrizales
// Professor:   Greg Ozbirn
// Date:        10/29/2022
// Class:       CS 4348.001

import java.util.concurrent.Semaphore;

public class DMVSimulation {
    //The number of customers that enter DMV
    public static final int NUM_CUSTOMERS = 20;

    //--------------------------------Semaphores------------------------------
    //Full description for each in the design file.
    static Semaphore agentServing = new Semaphore(0, true);
    static Semaphore customerTicketRequest = new Semaphore(0, true);
    static Semaphore DMVagent[] = {new Semaphore(0, true), new Semaphore(0, true)};
    static Semaphore ticketGiven = new Semaphore(0, true);
    static Semaphore agentAvailable = new Semaphore(0, true);
    static Semaphore announceCustomerTicket = new Semaphore(0, true);
    static Semaphore customerInWaitingArea = new Semaphore(0, true);
    static Semaphore customerReadyEyePhoto[] = {new Semaphore(0, true), new Semaphore(0, true)};
    static Semaphore customerInAgentLine = new Semaphore(0, true);
    static Semaphore customerReadyLicense = new Semaphore(0, true);
    static Semaphore customerLeaves = new Semaphore(0, true);
    static Semaphore agentLine = new Semaphore(4, true); //only 4 people can be in the agent line
    static Semaphore infoDeskLine = new Semaphore(5, true);
    static Semaphore waitingArea = new Semaphore(20, true);
    static Semaphore licenseGiven[] = {new Semaphore(0, true), new Semaphore(0, true)};
    static Semaphore customerLicenseRequest[] = {new Semaphore(0, true), new Semaphore(0, true)};
    static Semaphore mutex = new Semaphore(1, true);
    static Semaphore eyeAndPhotoTaken[] = {new Semaphore(0, true), new Semaphore(0, true)};

    //Other variables used
    static int ticket_number = 0; //stores the ticket number
    static int customerLicenseRequestID[] = new int[2]; //stores the customer id that will be passed to the agent

    public static void main(String[] args) {

        // Initialize Threads
        Thread informationDesk = new Thread(new InformationDesk()); //initialize information desk thread
        Thread announcer = new Thread(new Announcer()); //initialize announcer thread
        Thread agent[] = {new Thread(new Agent(0)), new Thread(new Agent(1))}; //initialize agent thread (two agents)
        Thread customer[] = new Thread[NUM_CUSTOMERS]; //initialize customer thread (will have NUM_CUSTOMER threads)

        for(int i = 0; i < NUM_CUSTOMERS; i++) {
            customer[i] = new Thread(new Customer(i));
        }

        // Start each of the threads
        informationDesk.start();
        announcer.start();
        agent[0].start();
        agent[1].start();
        for(int i = 0; i < NUM_CUSTOMERS; i++) {
            customer[i].start(); //start the customer threads
        }

        // Join customer threads
        for(int i = 0; i < NUM_CUSTOMERS; i++) {
            try {
                customer[i].join(); //join the customer threads when they leave the DMV
                System.out.println("Customer " + i + " was joined");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        //stop DMV worker threads from running
        informationDesk.interrupt();
        announcer.interrupt();
        agent[0].interrupt();
        agent[1].interrupt();

        System.exit(0); // after all customer threads leave the DMV we exit the simulation

    } //end of main

    //Thread that simulates a Customer
    public static class Customer implements Runnable
    {
        int id; //customer id
        public Customer(int id)
        {
            this.id = id;
        }

        public void run()
        {
            try {
                infoDeskLine.acquire(); //wait to get inside the DMV (in case info desk line is full)

                System.out.println("Customer " + id + " created, enters DMV"); //enters the DMV

                //Get ticket from Information Desk and go to waiting area
                mutex.acquire(); //will prevent multiple threads from getting mixed up or grabbing same ticket number
                customerTicketRequest.release(); //signal information desk that customer wants ticket
                ticketGiven.acquire(); // wait for information desk to give the ticket number
                System.out.println("Customer " + id + " gets number " + ticket_number + ", enters waiting room");
                waitingArea.acquire(); // customer enters the waiting area
                infoDeskLine.release(); //signal the information desk line that it has left the line
                mutex.release(); //allows the next customer to use information desk (mutex prevents customer mix up)

                //Wait for announcer to call ticket
                customerInWaitingArea.release(); //signal announcer that it (the customer) is in the waiting area
                announceCustomerTicket.acquire(); //wait for ticket number to be called by the announcer

                //Once ticket is called, go to the agent and wait for an agent
                System.out.println("Customer " + id + " moves to agent line");
                waitingArea.release(); //signals that it has left the waiting area
                customerInAgentLine.release(); //signal announcer that he is in the agent line
                agentAvailable.acquire(); //wait for agent to be available

                //When an agent is available we want to determine who it is and go to them
                int agent_num; //store the agent

                if(!DMVagent[0].tryAcquire()) {
                    DMVagent[1].acquire(); //wait for DMV agent 1 if agent 0 is not available
                    agent_num = 1; //customer will thus get agent 1
                }
                else {
                    agent_num = 0; //else if DMV agent 0 is available, they get agent 0
                }

                //Tell agent who they are, request a license
                customerLicenseRequestID[agent_num] = id; //will tell the agent the customer id (i.e., customer 1, 2, 3, etc)
                customerLicenseRequest[agent_num].release(); //signal the agent that they want a driver's license

                //At this point the customer is being served by an agent
                agentServing.acquire(); //wait for agent to accept driver's license request and begin serving
                agentLine.release(); //signal the announcer that has left agent line
                System.out.println("Customer " + id + " is being served by agent " + agent_num);

                //Customer will then take eye exam and photo
                customerReadyEyePhoto[agent_num].release(); //signal that customer is ready for eye exam and photo
                eyeAndPhotoTaken[agent_num].acquire(); //wait for eye exam and photo to be taken
                System.out.println("Customer " + id + " completes photo and eye exam for agent " + agent_num);

                //Customer is now ready to receive their license and finish the interaction with the patient
                customerReadyLicense.release(); //signal agent that customer is ready to get license
                licenseGiven[agent_num].acquire(); //wait to be done with agent (wait to get driver's license )
                System.out.println("Customer " + id + " gets license and departs");

                customerLeaves.release(); //customer leaves the DMV

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } // end of run
    } // end of customer class

    //Thread that simulates the Information Desk
    public static class InformationDesk implements Runnable {
        @Override
        public void run() {
            try {
                System.out.println("Information desk Created");
                while (true) {
                    //Information Desk waits for a customer to request a ticket number and gives them one
                    customerTicketRequest.acquire(); //wait for customer come request a ticket

                    ticketGiven.release(); //signal customer that they have given them their ticket

                    ticket_number += 1; //increase the ticket number for the next customer

                } // end of while loop
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } // end of run
    } // end of information desk class

    //Thread that simulates the Announcer
    public static class Announcer implements Runnable {
        int number = 1; //the announcer number which starts at 1
        @Override
        public void run() {
            try {
                System.out.println("Announcer created");
                while (true) {
                    //Announcer waits for people to arrive to the waiting and for agent line to have a spot
                    customerInWaitingArea.acquire(); // wait for there to be a customer in the waiting area
                    agentLine.acquire(); //wait for there to be a spot in the agent line (only 4 spots maximum)

                    //Announcer calls next ticket number
                    System.out.println("Announcer calls number " + number); //calls next customer number
                    announceCustomerTicket.release(); // signal next ticket number announcement

                    //Wait for customer to go in line before next announcement
                    customerInAgentLine.acquire(); //wait for the customer to go to agent line

                    number +=1; //increase announcement number for next customer

                } // end of while loop
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } // end of run
    } // end of Announcer class

    //Thread that simulates an agent
    public static class Agent implements Runnable {

        int id; //agent id (0 or 1)
        public Agent(int id) {
            this.id = id;
        }

        @Override
        public void run() {
            try {
                System.out.println("Agent " + id + " created");
                while (true) {
                    //There are two DMV agents and any of them can grab a customer

                    //Accept the next customer in the agent line
                    DMVagent[id].release();
                    agentAvailable.release();

                    //Get the customer's driver's license request and their information (what customer they are)
                    customerLicenseRequest[id].acquire();//wait for customer
                    int customerID = customerLicenseRequestID[id]; //get the customers ID
                    System.out.println("Agent " + id + " is serving customer " + customerID);
                    agentServing.release(); //signal customer they are being served and will need to go through some steps (exam and photo)

                    //The agent will ask customer to take their eye exam and photo
                    customerReadyEyePhoto[id].acquire(); //wait for customer to be ready for photo and eye exam
                    System.out.println("Agent " + id + " asks customer " + customerID + " to take photo and eye exam");
                    eyeAndPhotoTaken[id].release(); //signal customer that eye and photo are being taken

                    //Now the driver's license can be given to the customer
                    customerReadyLicense.acquire(); //wait for customer to be ready for license
                    System.out.println("Agent " + id + " gives license to customer " + customerID);
                    licenseGiven[id].release(); //signal the customer that they have been given their license

                    customerLeaves.acquire(); //wait for the customer to leave

                } // end of while loop
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } // end of run
    } // end of Announcer class

} // end of public class project2
