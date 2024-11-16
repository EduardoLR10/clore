# clojure-demo

A demo application using Clojure.

## Usage

FIXME: explain things, add license, basically make this an actual thing.
For now, refer to the docstrings in the code for details.

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

Run the tests:

    $ clojure -T:build test

TODO: add tests.

Run the project's CI pipeline and build an uberjar:

    $ clojure -T:build ci

This will produce an updated `pom.xml` file with synchronized dependencies inside the `META-INF`
directory inside `target/classes` and the uberjar in `target`. You can update the version (and SCM tag)
information in generated `pom.xml` by updating `build.clj`.

If you don't want the `pom.xml` file in your project, you can remove it. The `ci` task will
still generate a minimal `pom.xml` as part of the `uber` task, unless you remove `version`
from `build.clj`.

Run that uberjar:

    $ java -jar target/clojure-demo/clojure-demo-0.1.0-SNAPSHOT.jar
