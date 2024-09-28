# Boardgames Exercise

Published at https://neuroning.com/boardgames-exercise/

---

This repo is a solution to exercise 2.13 of book _"Software Design for
Flexibility"_ by by Chris Hanson and Gerald Jay Sussman. In Clojure.

__Assignment__: Model the rules of Chess. Design the code with the
 flexibility to easily add new types of pieces with unique movements. The
 goal is to create a shared core model that can also be used for other
board games, such as Checkers, Tic-Tac-Toe, and similar.

The code includes [Clerk](https://github.com/nextjournal/clerk) notebooks with a code walkthrough of the implementation with visualizatons and code stepper.

## To start Clerk notebooks locally
Install clj and babashka. Then run:

```sh
bb clerk-watch
```

This will start the Clerk server at http://localhost:7778 with a file
watcher that updates the page each time any file in the `src` directory changes.

Alternatively, you may want to start a Clojure REPL activating the `:dev` alias
(`clj -M:dev`), connect with your editor and then call `(user/start!)` to start
the Clerk server.

## To run all tests

```
bb test
# or:
bb test --skip-meta :failing-on-purpose
```

## To build the static site

```
bb build-static
```
Generates the static site with all code and notebooks. The output goes under `public/`

## License

Copyright © 2024 Fernando Dobladez

Distributed under GNU GENERAL PUBLIC LICENSE version 3
