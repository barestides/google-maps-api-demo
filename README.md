# Google Maps API Demo

Some CLJS experimentation making use of the Google Maps API

## Setup

First, add your Google Maps API key in `resources/public/index.html` at line 13
where it says `API-KEY-HERE`.

### Production

To create a production build run:

    lein do clean, cljsbuild once min

And open your browser in `resources/public/index.html`. You will not
get live reloading, nor a REPL.

### Development

To get an interactive development environment run:

    lein figwheel

and open your browser at [localhost:3449](http://localhost:3449/).
This will auto compile and send all changes to the browser without the
need to reload. After the compilation process is complete, you will
get a Browser Connected REPL. An easy way to try it is:

    (js/alert "Am I connected?")

and you should see an alert in the browser window.

To clean all compiled files:

    lein clean

## License

Copyright © 2018 Braden Arestides

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
