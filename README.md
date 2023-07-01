# This project studies the coordination of multiple threads using semaphores by simulating operations at the Department of Motor Vehicles for customers renewing their driver’s license.  

The threads used are as follows (more detailed information in the simulation design file):

Customer
1)	20 customers (1 thread each) all created at the start of the simulation.
2)	Waits in line at Information Desk to get a number.
3)	Waits in waiting area until number is called.
4)	Waits in line for agent.
5)	Works with agent to complete driver’s license application.
6)	Exits

Information Desk
1)	1 thread created at the beginning.
2)	Assigns a unique number sequentially starting at 1 to each customer.

Announcer
1)	1 thread created at the beginning.
2)	Tries to keep agent line filled with 4 people.

Agent
1)	2 threads created at the beginning.
2)	Asks customer to take eye exam and photo.
3)	Provides customer with temporary license.

Main
1)	Creates and joins all customer threads.  Customer threads may be joined in creation order.
2)	When last customer has exited, ends the simulation.
 
Other rules:
1)	Each activity of each thread should be printed with identification (e.g., customer 1).
2)	All mutual exclusion and coordination must be achieved with semaphores.  
3)	A thread may not use sleeping as a means of coordination.  
4)	Busy waiting (polling) is not allowed. 
5)	Mutual exclusion should be kept to a minimum to allow the most concurrency.
6)	The semaphore value may not obtained and used as a basis for program logic.
7)	Each customer thread should print when it is created and when it is joined.
8)	All activities of a thread should only be output by that thread.
