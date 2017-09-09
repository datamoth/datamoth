[![Build Status](https://travis-ci.org/datamoth/datamoth.svg?branch=master)](https://travis-ci.org/datamoth/datamoth)

### What is datamot?

Datamot simplifies creating hadoop batch processing pipelines and solves most
of the infrustructural issues that appear when deploying jobs to Hadoop/Oozie.

### Ok... So what is it?

Datamot helps you to organize your analytical code into a project and to deploy
it to a hadoop cluster. You don't need to copy files to HDFS and substitute
variables in configuration. No scripts boilerplate. Just push your code to git
as you do it always and let the magic happen.

### Uhmm.. anything else?

Automatic start/stop of jobs. Easy, centralized and local configuration of the
project. Configuration profiles (production, test, anything). Freedom in project
structure.

### Is this a marketing tool? You speak like a salesman.

Totally not. We are far from marketing, we are engineers. Just install the first
version of datamot and you'll get 50% discount for the next version.

### What? Isn't this tool for free?

It's free and it's free for commercial use.

### Well.. Ok, how do I install it?

Datamot consists of two parts, cli tool (which you may use, but totally not
have to) and super simple web UI. When you run the web UI for the first time
it'll create bare git repositories, each per project listed in configuration.

Then you just push your code to that repos and it gets deployed to a cluster.
All the cluster configuration is kept in the special conf file in your project.
After push you can navigate your project (datasets, coordinators, workflows,
code) through the web UI, which greatly helps when troubleshooting code bugs.

### Stop! Please, stop. Just say how to install it.

Download the distro. Unpack it. Run the `datamot/bin/datamot --start`. Go to
http://localhost:2718.

### That's better. Now how to install it, the right way?

Normally you need to install datamot to some server which is available to push
via ssh for you and your teammates. Your hadoop cluster and it's services, like
Oozie, HDFS need to be available from this server. And it would be just nice to
create a dedicated user on that server to run datamot.

After creating a user (or deciding to use an existing one) unpack datamot
to whatever place you want, and add it's `bin` folder to paths. So, if, for
example, you unpack it to `/home/datamot/datamot`, then you need to fix $PATH
environment variable like this:

`$PATH=/home/datamot/datamot/bin/:$PATH`

Now `datamot --version` should work from anywhere. If your are going to use UI
(we adwise you to use it) just run `datamot --start`. Currently we are working
on installing datamot as a service. For now you can use screen to run detached
datamot like this:

`screen -dmS datamot datamot --start`

You can change default ip and port on which datamot will listen. Take a look at
`conf/datamot.conf`.

`remoteUser` is a ssh user under which you are going to push to git,
`remoteHost` is a host to push,

these two variables are needed to show push URL in the user interface.

### Is it production ready, by the way?

We use it on production, but on a cluster with not so many (but quite heavy)
jobs: about 30 coordinators. We are now in early alpha state and need to
gather more feedback from users. Looking forward to your comments.

### Got it up and running, what's next?

Let's create a simple coordinator...

### Wait... What's a coordinator?

Ok, there are several workflow managers for Hadoop. One of the best (by our
opinion) is Oozie. You know, it's like a container for you scripts (python, pig,
hive-sql, whatever) that runs periodically by a scheduler.

### Why Oozie?

It's declarative and perfectly fits to a modern view of immutable architecture.
It has all the necessary entities to help you model your calculations: datasets,
workflows, coordinators, coordinator bundles (pipelines) and dependencies
between them.

### So, what's a coordinator?

You'd better look at the official Oozie docs. It's a great tool! Here you go:
http://oozie.apache.org/ .

### Got it, how to run something on cluster, eventually??

We'll prepare a toy project for Hadoop/Oozie/Spark/Hive stack with several
coordinators and simple toy calculations within them soon. Just clone it and
push to the URL shown in the menu of the datamot user interface. But before the
push fix .conf file in the root to match your cluster configuration.

### Wow. I need a feature.

Cool! Create an issue!

### Help. I need help.

Use issues. You can join us in [Telegram](https://t.me/datamot) as well.
