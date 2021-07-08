# Site Index Spider

A command line appliction to scan a small or medium sized website and generate a site map report.

## Building the application

Build the application with the gradle wrapper:

```bash
./gradlew clean build
```

This will create the jar `build/libs/siteindex-0.0.1-SNAPSHOT.jar`

## Running the application

Use the `siteindex.sh` wrapper to launch the application.

> Usage: siteindex.sh '<homepage-url>' '<output-file>'

e.g.

```bash
./siteindex.sh 'https://oliver-pavey.appspot.com/' './oliver-pavey-siteindex.html'
```

> The single quotes are necessary because the first parameter contains a colon, 
> which bash uses to split commands. 

## Key source code

### `siteindex.sh`

Bash script which sets the required environment variables, and invokes the java program.

### `SiteindexAutorun.java`

This implements `CommandLineRunner` and orchestrates:

- The processing of the environment variables determining the site to scan and the output filename.
- Calls the `SiteScanner` to scan the website.
- Invokes the Thymeleaf template (with no webserver) to generate the output.
- Cleans and saves the output to a file.

### `SiteScanner.java`

This is the class which scans the website starting from the supplied homepage.
It returns a model which can be passed into the report.

### `report_template.html`

This is the Thymeleaf template which formats the model into a report.
