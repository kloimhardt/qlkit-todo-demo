# qlkit-todo-demo with Phel backend

This is a "batteries included" demo of [qlkit](https://github.com/forward-blockchain/qlkit), the no-dependencies, 300 loc GraphQL-inspired client-server web library for Clojurescript. This particular fork and brach provides a [Phel](https://phel-lang.org) backend in addition to its Clojure counterpart. Try a live version [here](http://www.kloimwieder.com/).

The core of the qlkit backend is only 60 loc, the according [Phel code](https://github.com/kloimhardt/qlkit-todo-demo/blob/phel-backend-2/resources/public/phel-backend/qlkit/core.phel) is copied from the original Clojure version without notable changes. This [recommended qlkit introductory article](https://medium.com/p/79b7b118ddac) provides a walkthrough of the original application.

## Setup for Clojurescript development

To get an interactive development environment run:

    clojure -m figwheel.main -b dev -r

and open your browser at [localhost:9500](http://localhost:9500/).
This will auto compile and send all changes to the browser without the
need to reload. After the compilation process is complete, you will
get a Browser Connected REPL. An easy way to try it is:

    (js/alert "Am I connected?")

and you should see an alert in the browser window.

## Setup for additional Phel backend

Copy `composer.phar` into resources/public (more about Composer [here](https://getcomposer.org))

```
cd resources/public
php composer.phar install
php -S localhost:8000
```

Open http://localhost:8000/ (you can close http://localhost:9500/ now).

## Clojurescript compilation for poduction

```
clj -m figwheel.main -O advanced -bo dev
```

---
_Copyright (c) Conrad Barski. All rights reserved._
_The use and distribution terms for this software are covered by the Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php), the same license used by Clojure._

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
