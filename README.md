# clore

A demo application using Clojure.

This project is inspired by [rexim's tore project](https://github.com/rexim/tore); an application of reminders and notifications.
Our focus is on showing the **reminders** in the browser via server-side rendered HTML.

## Libraries

- [datomic](https://www.datomic.com/) - Database Management
- [http-kit](https://http-kit.github.io/) - HTTP Server
- [init](https://github.com/ferdinand-beyer/init) - Dependency Injection
- [ring](https://github.com/ring-clojure/ring) - HTTP Framework
- [rum](https://github.com/tonsky/rum) - HTML Rendering

## Resources

We are currently grabbing our CSS from [rexim's project](https://github.com/rexim/tore/blob/main/resources/css).

## Usage

Run the development REPL and start the server:

    $ clj -M:dev
    user=> (go)

You may want to run `clj -M:dev:cider` or `clj -M:dev:nrepl` instead depending
on your editor of choice.

Run in production mode:

    $ clojure -M:run

Format the code:

    $ clojure -M:format fix

You can also run `clojure -M:format` for additional options. We use `cljstyle`
for formatting, and it is recommended that you install the CLI version if
you plan on hooking this up to your editor.

Run this command to build an uberjar:

    $ clojure -T:build uberjar

Run that uberjar:

    $ java -jar target/clore/clore-0.1.0-SNAPSHOT.jar
