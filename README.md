# client-server-proj (this project is implemented in java - therefore installed JDK and JVM are required).

Build instructions:
-------------------

- run ./build.sh
ex., $ ./build.sh

Run instructions:
-----------------

- run ./run.sh with 1 argument: full path of file to serve text from
ex., $ ./run.sh /tmp/foobar


Design:
-------

TextProvider (Main class)

	- calculate line count from input file
	- creates and start ConnectionHandler thread
	- enter accept connection loop: 
		: accept and queue connections to connection queue
		: continue while ConnectionHandler thread is alive

ConnectionHandler

	- create and start pool of 4 JobWorker threads
	- enter connection loop: 
		: remove connection from connection queue, create job and add to job worker queue
		: continue while all job worker threads are alive

JobWorker

	- outer loop: get job from job worker queue
		: continue until shutdown command is received
		- inner loop: read and process commands from socket
			: continue until quit command is received
				- process command, send response
			: when quit command is received, close socket, exit inner loop
		: when shutdown command is received, exit out loop and return from job worker


Job Queue

	- protected from concurrent access by exlusive access through synchronized method getNextJob().


Performance:
------------

- performance with 1GB, 10GB and 100GB input files:

The data from the input file is obtained by issuing the shell command "cat /tmp/1G | sed -n '{line#}p'".  One 
observation from testing with large files is that the data of interest is returned well before the command pipeline 
completes.  The second observation is that the execution time increases more or less linearly in relation to the file size.

$ time cat /tmp/1G | sed -n '10001p'
the legitimate rights and interests of Chinese companies.”

real	0m5.535s
user	0m5.251s
sys	0m0.688s

$ time cat /tmp/10G | sed -n '10001p'
the legitimate rights and interests of Chinese companies.”

real	0m54.082s
user	0m52.753s
sys	0m9.389s
$ time cat /tmp/100G | sed -n '10001p'
the legitimate rights and interests of Chinese companies.”

real	9m13.132s
user	8m57.637s
sys	1m18.171s

The findings suggest two optimizations:

1) a custom search command could improve performance by returning as soon as the requested data is returned

2) significant performance improvements could be obtained by partitioning large files, using the requested
line number to quickly determine which file partition to search in.

- performance as the number of client request/second increases:

The number of worker threads (4) is chosen to optimize the 4 virtual CPUs on the target system.  

The main thread and the connection-handler threads should be able to keep up with a high volume of requests
because their work is primarily to queue up jobs for other threads to handle.

As the number of client requests/second increases, the worker threads will have more and more difficulty
processing job queue entries quickly enough to prevent the queue size from continually increasing, particularly
if a very large text file is being served.  

As mentioned about, optimizations with file serving can be achieved with a custom search command and pre-processing
large input files into indexable partitions.  The optimizations would be necessary to prevent uncontrolled job
queue growth when both large input file sizes and rapid requests/second conditions occur in conjunction with each
other.


Time spent:
-----------

- although I didn't keep track, total time spent was probably between 8 and 15 hours in design, coding, testing
and debugging.  Additional time was spent developing/testing/debugging a test client.


