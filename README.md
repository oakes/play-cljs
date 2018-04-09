## Introduction

A ClojureScript library for making games. It uses [p5.js](http://p5js.org/) underneath.

### [Try the interactive docs!](https://oakes.github.io/play-cljs/)

There are several ways to create a project:

* [Boot](http://boot-clj.com/): `boot -d boot/new new -t play-cljs -n hello-world`
* [Nightcode](https://sekao.net/nightcode/): Choose "Game Project" from its start menu
* [Nightcoders.net](http://nightcoders.net/): Choose "Game" when creating a new project
* [Lightmod](https://sekao.net/lightmod/): Choose one of the game templates

## Documentation

* Check out [the example games](https://github.com/oakes/play-cljs-examples)
* Read [the dynadocs](https://oakes.github.io/play-cljs/)
* Join the discussion on [r/playcljs](https://www.reddit.com/r/playcljs/)
* Look at this commented example:

```clojure
(ns hello-world.core
  (:require [play-cljs.core :as p]))

(defonce game (p/create-game 500 500))
(defonce state (atom {}))

; define a screen, where all the action takes place
(def main-screen
  (reify p/Screen
  
    ; runs when the screen is first shown
    (on-show [this]
      ; start the state map with the x and y position of the text we want to display
      (reset! state {:text-x 20 :text-y 30}))

    ; runs when the screen is hidden
    (on-hide [this])

    ; runs every time a frame must be drawn (about 60 times per sec)
    (on-render [this]
      ; we use `render` to display a light blue background and black text
      ; as you can see, everything is specified as a hiccup-style data structure
      (p/render game
        [[:fill {:color "lightblue"}
          [:rect {:x 0 :y 0 :width 500 :height 500}]]
         [:fill {:color "black"}
          [:text {:value "Hello, world!" :x (:text-x @state) :y (:text-y @state) :size 16 :font "Georgia" :style :italic}]]])
      ; increment the x position of the text so it scrolls to the right
      (swap! state update :text-x inc))))

; start the game
(doto game
  (p/start)
  (p/set-screen main-screen))
```

## Licensing

All files that originate from this project are dedicated to the public domain. I would love pull requests, and will assume that they are also dedicated to the public domain.
