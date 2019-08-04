# ocr-with-aws
Preparations:
AWSService Class:
- create crediential and save the Acess key ID and Secret access key in c:users/username/.aws/cerdentials.
- create Role and save the Role ARN at the AWSService Class.
- create Key Pair, save it at the AWSService Class.
- create Secirty Group, save the group name at the AWSService Class.
- use ami-08fe4614a9f89c0ec, save it at the AWSService Class.
AWS Account:
- create in S3 two buckets with the names and upload the jar files:
	- name: workerbucketdistributed 
	  jar file: worker.jar
	- name: managerbucketdistributed 
	  jar file: manager.jar

HOW THE PROGRAM WORKS:

Application:
The application checks if a manager is active and if not, starts it with a jar file located in managerbucketdistributed bucket called manager.jar.
The manager has input tasks queue which it accepts tasks through it.
The application checks if this queue exists and accordingly knows if the manager is active or no.
The application uploads the images url's file to a bucket called - appbucketdistributed which the manager has created for input tasks from applications.
In case there is a file with same name in appbucketdistributed the user will get an error message that he need to give a uniqe name.
The application create a queue which through it it will get the result message from the manager, let call it resultQueue.
Then, it send a Task message to the manager through input tasks queue that contains the name of the file, number of tasks per worker and the resultQueue url.
The application waits to gets a DoneTask meesage in resultQueue which contains the location of result file - bucket name and url of the file.
When it gets the messge it creats html file from the images url and the text results.
The the application delete the input file from appbucketdistributed and the resultQueue.

Manager:
First the manager creates 2 buckets:
1. appbucketdistributed - where the applications add the input files.
2. resultimagestextbucketdistributed - where the manager put the result files.
In addition it creates queue for input tasks for messages from applications.
The manager has inner counter and it gives a appId for every application.
The manager has executer (Thread pool) and for every task from an application, the manager give the pool a runnable task (AppTasker Class) which do:
Create 2 queues, one for image tasks and second for done image tasks. (the queues' names include the appId because we want to have 2 queues per application). 
Through image tasks queue the workers will get the task's information and execute the ocr algorithem.
To the done task queue the workers will send the result to the manager.
It downloads the images url file from s3 (according to the name it gets in Task) and for every line in the file it creates image task which contains the image url.
Then it creates number of workers (instances) according to the number of images per worker the manager got from the Task message and the amount of the images in the input file.
It creates the workers inatances with a jar file located in workerbucketdistributed bucket called worker.jar.
For every message from the done image task queue it creates output string that includes image url line and text result line. 
When it gots amount of messages same as amount of images, it uploads the result as text file in resultimagestextbucketdistributed and creates Done Task message which contains the result file url and the bucket name the file is located.
The Done Task sends through the resultQueue that the application sends.
Then it delete the workers and the queues it created for the application Task.

Worker:
The worker gets 3 arguments:
1. image task queue url - mark it as taskQueue
2. done image task queue utl - mark it as doneQueue
3. number of image tasks to execute -  mark it as N
Then it loops for N times and for each time it takes an image task from taskQueue which includes inage url, execute ocr algorithem for it and create done task which includes the image url and the text result from ocr and send it to doneQueue.

Pay attention:
The application should give an input file name different than files which already exists in appbucketdistributed because we do not want applications overwrite files that belongs to other applications.
