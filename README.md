# Cosla

Calculate SLA metrics from your JIRA project.

## Features

Cosla is a command-line tool that extracts data into CSV reports from Jira tickets via Jira REST API.

Example Reports:

- *Time to Close* The length of time (in business days) it took for each Jira ticket to reach "closed" state.
- *Open Per Day* Number of Jira tickets open on each calendar day.
- *Time in Status* For each ticket, report the time it has spent in each of its statuses.
- *Audit Statues* (No CSV, console output only) For the configured JQL, print the statuses used by all matching tickets.

For all the CSV reports, Cosla defines "Closed" using ticket "Status" rather than "Resolution."  For example, if you decide that Jira tickets are "closed" when they reach the "Awaiting Approval" status (i.e. your deveopers submit a ticket for stakeholder approval), you can reflect that in your Cosla Reports. This is expecially useful for workflows (such as Jira's default workflow) where an issue is not actually closed when it is "Resolved."

Cosla supports a holiday calendar (via configuration) so that weekends and holidays don't count towards a Jira tickets resolution time.

## Prerequsites

To build, you need to install Java and Lieningen:

    http://leingingen.org

If you have a pre-built jar already, you only need Java to run with `java -jar` - no need for Leiningen. See below for instructions.

Finally, your Jira instance must supprt the [Jira REST API](https://developer.atlassian.com/jiradev/jira-apis/jira-rest-apis), and you must have a login.

## Usage

1. Inspect the example configuration file, `example.config.edn`. At a minimum, you'll need to edit your username, password and the JQL (Jira Query Language) clause to match you desired project. NOTE: You can export your password the environment variable `JIRA_SLA_PASSWORD`. Cosla will detect and use that if present.

2. Run one of the reports with:

    $ lein run -m cosla.main <report-name>

3. You can also compile to an uberjar (all dependencies included) with:

    $ lein uberjar

and then run with

    $ java -jar cosla-<version>-standalone.jar <report-name>


## What Reports are Available?

Inspect the source code, or run Closa without arguments to view options:

    $ lein run -m cosla.main

## Acknowledgements

I used the following projects/blogs as a guide for using the JIRA API from Clojure:

- https://github.com/mrroman/jira-client
- http://maurits.wordpress.com/2012/03/02/extracting-data-from-jira-using-clojure-and-rest/

Thank you to Brian KimJohnson for suggesting the name Cosla.

Cosla was developed in vim using Tim Pope's [vim-fireplace](https://github.com/tpope/vim-fireplace) plugin. Check it out!

## COPYRIGHT AND LICENSE

This application was developed by Noah Zucker (@noahlz) at Novus Parters, and copywrite of code prior to September 2017 belongs to Novus Partners. Subsequent contributions belong to their respective authors.

Copyright © [Novus Partners, Inc.](https://www.novus.com)

Distributed under the Eclipse Public License (1.0). See LICENSE file.
