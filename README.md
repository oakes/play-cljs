## Introduction

A ClojureScript library for making games. It uses [Pixi.js](http://www.pixijs.com/) for rendering. You can create a new play-cljs project with the template:

```
boot -d seancorfield/boot-new new -t "play-cljs" -n "hello-world"
```

## Documentation

* Read the source (I know that sucks, but I am in very early stages of development!)
* Check out [the example games](https://github.com/oakes/play-cljs-examples)
* Join the discussion on [r/playcljs](https://www.reddit.com/r/playcljs/)
* Look at this commented example:

```clojure
; define a screen, where all the action takes place
(def main-screen
  (reify p/Screen
    ; all screen functions get a map called "state" that you can store anything inside of
    ; the return value must be one or more "commands", or nil if you don't want to run any commands
    
    ; "commands" are any record that implements the Command protocol
    ; they represent an action that play-cljs will execute on your behalf
    ; you can build your own, but here are the built-in ones:
    
    ; `reset-state` changes the contents of the state map
    ; `graphics` generates shapes with a hiccup-like syntax
    ; `sprite` displays an image
    ; `movie-clip` plays a sequence of sprites
    ; `text` displays a string

    ; runs when the screen is first shown
    (on-show [this state]
      (p/reset-state
        (assoc state
          :label (p/text "Hello, world!" {:x 0 :y 0 :fill 0xFFFFFF})
          :background (p/graphics
                        [:fill {:color 0x8080FF :alpha 1}
                         [:rect {:x 0 :y 0 :width view-size :height view-size}]])})))

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
