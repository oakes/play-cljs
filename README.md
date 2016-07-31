## Introduction

A ClojureScript library for making games.

## Documentation

* Check out [the example games](https://github.com/oakes/play-cljs-examples)
* Look at this commented example:

```clojure
; define a screen, where all the action takes place
(def main-screen
  (reify p/Screen
    ; all screen functions get a map called "state" that you can store anything inside of
    ; the return value must be one or more "commands", or nil if you don't want to run any commands

    ; runs when the screen is first shown
    (on-show [this state]
      (p/reset-state {:label (p/text "Hello, world!" {:x 0 :y 0 :fill 0xFFFFFF})
                      :background (p/graphics
                                    [:fill {:color 0x8080FF :alpha 1}
                                     [:rect {:x 0 :y 0 :width 500 :height 500}]])}))

    ; runs when the screen is hidden
    (on-hide [this state])

    ; runs every time a frame must be drawn (about 60 times per sec)
    (on-render [this state]
      [(:background state)
       (:label state)])

    ; runs whenever an event you subscribed to happens (see below)
    (on-event [this state event]
      (case (.-type event)
        "keydown" (.log js/console "You typed something!")
        "mousemove" (.log js/console "You moved your mouse!")))))

; get a canvas element from the page
(def canvas (.querySelector js/document "#canvas"))

; get a renderer object
(defonce renderer
  (p/create-renderer 500 500 {:view canvas}))

; get a game object, stop it if necessary (for reloading), and then start it
(defonce game (p/create-game renderer))
(doto game
  (p/stop)
  (p/start ["keydown" "mousemove"]) ; subscribe to keydown and mousemove events
  (p/set-screen main-screen))
```

## Licensing

All files that originate from this project are dedicated to the public domain. I would love pull requests, and will assume that they are also dedicated to the public domain.
