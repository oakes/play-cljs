## Introduction

A ClojureScript library for making games. It uses [p5.js](http://p5js.org/) underneath. You can create a new play-cljs project with the template:

```
boot -d seancorfield/boot-new new -t "play-cljs" -n "hello-world"
```

## Documentation

* Read the source (I know that sucks, but I am in very early stages of development!)
* Check out [the example games](https://github.com/oakes/play-cljs-examples)
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
          ["Hello, world!" {:x (:text-x @state) :y (:text-y @state) :size 16 :font "Georgia" :style :italic}]]])
      ; increment the x position of the text so it scrolls to the right
      (swap! state update :text-x inc))

    ; runs whenever an event you subscribed to happens (see below)
    (on-event [this event]
      (case (.-type event)
        "keydown" (.log js/console "You typed something!")
        "mousemove" (.log js/console "You moved your mouse!")))))

; start the game and listen to the keydown and mousemove events
(doto game
  (p/stop)
  (p/start ["keydown" "mousemove"])
  (p/set-screen main-screen))
```

## Licensing

All files that originate from this project are dedicated to the public domain. I would love pull requests, and will assume that they are also dedicated to the public domain.
