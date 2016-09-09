#!/usr/bin/env boot

(set-env! :dependencies '[[org.clojure/core.async "0.2.385"]
                          [codox "0.9.7"]
                          [hiccup "1.0.5"]
                          [markdown-clj "0.9.89"]])
(require
  '[clojure.string :as str]
  '[codox.reader.clojurescript :refer [read-namespaces]]
  '[hiccup.core :refer [html]]
  '[markdown.core :refer [md-to-html-string]])

(def core (or (->> (read-namespaces "src/play_cljs")
                   (filter #(= (:name %) 'play-cljs.core))
                   first
                   :publics)
              (throw (Exception. "Couldn't find the namespace"))))

; Game

(def game (or (->> core
                   (filter #(= (:name %) 'Game))
                   first)
              (throw (Exception. "Couldn't find Game"))))

; Screen

(def screen (or (->> core
                     (filter #(= (:name %) 'Screen))
                     first)
                (throw (Exception. "Couldn't find Screen"))))

; Functions

(def create-game (or (->> core
                          (filter #(= (:name %) 'create-game))
                          first)
                     (throw (Exception. "Couldn't find create-game"))))

(def game-fns (->> game :members (sort-by :name)))

; Elements

(def elements [{:name :div}
               {:name :text}
               {:name :arc}
               {:name :ellipse}
               {:name :line}
               {:name :point}
               {:name :quad}
               {:name :rect}
               {:name :triangle}
               {:name :image}
               {:name :fill}
               {:name :stroke}
               {:name :bezier}
               {:name :curve}
               {:name :rgb}
               {:name :hsb}
               {:name :tiled-map}])

; Image

(def image {:name "Image"
            :doc "Image objects are returned by [load-image](#load-image)
and [pre-render](#pre-render). They can be placed in an :image element.

```
(def state (atom {}))

(def main-screen
  (reify p/Screen
    (on-show [this]
      (swap! state assoc :player-image (p/load-image \"player.jpg\")))
    (on-hide [this])
    (on-render [this]
      (p/render game [:image {:value (:player-image @state)}]))
    (on-event [this event])))
```"})

(def image-fns [{:name ".loadPixels"
                 :arglists '([image])
                 :doc "Loads the pixels data for this image into the pixels attribute."}
                {:name ".updatePixels"
                 :arglists '([image x y w h])
                 :doc "Updates the backing canvas for this image with the contents of the pixels array.

### Parameters

* `x`  x-offset of the target update area for the underlying canvas
* `y`  y-offset of the target update area for the underlying canvas
* `w`  width of the target update area for the underlying canvas
* `h`  height of the target update area for the underlying canvas"}
                {:name ".get"
                 :arglists '([image]
                             [image x y]
                             [image x y w h])
                 :doc "Get a region of pixels from an image.

### Parameters

* `x`  x-coordinate of the pixel
* `y`  y-coordinate of the pixel
* `w`  width
* `h`  height

### Returns

If no params are passed, those whole Image object is returned.
If x and y are the only params passed, the color of the pixel is returned in array
format: [R, G, B, A].
If all params are passed, a rectangle region is extracted and an Image object is returned.
If the region is outside the bounds of the image, nil is returned."}
                {:name ".set"
                 :arglists '([image x y a])
                 :doc "Set the color of a single pixel or write an image into this Image object.

Note that for a large number of pixels this will be slower than directly manipulating the pixels
array and then calling [updatePixels](#update-pixels).

### Parameters

* `x`  x-coordinate of the pixel
* `y`  y-coordinate of the pixel
* `a`  grayscale value | pixel array | a [Color](#Color) object | [Image](#Image) object to copy"}
                {:name ".resize"
                 :arglists '([image width height])
                 :doc "Resize the image to a new width and height. To make the image scale
proportionally, use 0 as the value for the wide or high parameter. For instance, to make
the width of an image 150 pixels, and change the height using the same proportion, use
`(.resize image 150 0)`.

### Parameters

* `width`  the resized image width
* `height`  the resized image height"}
                {:name ".copy"
                 :arglists '([image src-image sx sy sw sh dx dy dw dh])
                 :doc "Loads the pixels data for this image into the pixels attribute.

### Parameters

* `src-image`  source image object
* `sx`  X coordinate of the source's upper left corner
* `sy`  Y coordinate of the source's upper left corner
* `sw`  source image width
* `sh`  source image height
* `dx`  X coordinate of the destination's upper left corner
* `dy`  Y coordinate of the destination's upper left corner
* `dw`  destination image width
* `dh`  destination image height"}
                {:name ".mask"
                 :arglists '([image src-image])
                 :doc "Masks part of an image from displaying by loading another image and
using its blue channel as an alpha channel for this image.

### Parameters

`src-image`  source image object"}
                {:name ".filter"
                 :arglists '([image operation])
                 :doc "Applies an image filter to an Image object.

### Parameters

`operation`  one of \"threshold\", \"gray\", \"invert\", \"posterize\" and \"opaque\""}
                {:name ".blend"
                 :arglists '([image src-image sx sy sw sh dx dy dw dh blend-mode])
                 :doc "Copies a region of pixels from one image to another, using a
specified blend mode to do the operation.

### Parameters

* `src-image`  source image object
* `sx`  X coordinate of the source's upper left corner
* `sy`  Y coordinate of the source's upper left corner
* `sw`  source image width
* `sh`  source image height
* `dx`  X coordinate of the destination's upper left corner
* `dy`  Y coordinate of the destination's upper left corner
* `dw`  destination image width
* `dh`  destination image height
* `blend-mode`  the blend mode

Available blend modes are: normal | multiply | screen | overlay | darken | lighten | color-dodge |
color-burn | hard-light | soft-light | difference | exclusion | hue | saturation | color | luminosity"}
                {:name ".save"
                 :arglists '([image filename extension])
                 :doc "Saves the image to a file and force the browser to download it. Accepts two
strings for filename and file extension Supports png (default) and jpg.

### Parameters

* `filename`  give your file a name
* `extension`  \"png\" or \"jpg\""}])

; TiledMap

(def tiled-map {:name "TiledMap"
                :doc "TiledMap objects are returned by [load-tiled-map](#load-tiled-map).
They can be placed in a :tiled-map element.

The map must be exported from an editor like [Tiled](http://www.mapeditor.org/) in JavaScript
format. The resulting `.js` file must be included in the game's HTML before `main.js`. Lastly,
the name provided to `load-tiled-map` should coorespond to the name saved in that JS file.

```
(def state (atom {}))

(def main-screen
  (reify p/Screen
    (on-show [this]
      (swap! state assoc :map (p/load-tiled-map \"dungeon\")))
    (on-hide [this])
    (on-render [this]
      (p/render game [:tiled-map {:value (:map @state)}]))
    (on-event [this event])))
```"})

(def tiled-map-fns [{:name ".drawLayer"
                     :arglists '([tiled-map layer cam-left cam-top])
                     :doc "Draws a TiledMap layer.

### Parameters

* `layer`  layer index
* `cam-left`  left coordinate
* `cam-top`  top coordinate"}
                    {:name ".draw"
                     :arglists '([tiled-map cam-left cam-top])
                     :doc "Draws a TiledMap.

### Parameters

* `cam-left`  left coordinate
* `cam-top`  top coordinate"}
                    {:name ".getName"
                     :arglists '([tiled-map])
                     :doc "### Returns

The name of the map"}
                    {:name ".getVersion"
                     :arglists '([tiled-map])
                     :doc "### Returns

The version of the map"}
                    {:name ".getOrientation"
                     :arglists '([tiled-map])
                     :doc "### Returns

The orientation of the map"}
                    {:name ".getBackgroundColor"
                     :arglists '([tiled-map])
                     :doc "### Returns

The background [Color](#Color) of the map"}
                    {:name ".getMapSize"
                     :arglists '([tiled-map])
                     :doc "### Returns

The width and height of the map (in number of tiles) as a [Vector](#Vector)"}
                    {:name ".getTileSize"
                     :arglists '([tiled-map])
                     :doc "### Returns

The width and height of the tiles (in pixels) as a [Vector](#Vector)"}
                    {:name ".getHexSideLength"
                     :arglists '([tiled-map])
                     :doc "### Returns

The side length (only for hexagonal maps)"}
                    {:name ".getCamCorner"
                     :arglists '([tiled-map])
                     :doc "### Returns

The left and top corner coordinates as a [Vector](#Vector)"}
                    {:name ".getCamCenter"
                     :arglists '([tiled-map])
                     :doc "### Returns

The center coordinates as a [Vector](#Vector)"}
                    {:name ".getPosition"
                     :arglists '([tiled-map])
                     :doc "### Returns

Depending on dramMode, returns the camera corner or center coordinates.
Depending on positionMode, returns the Map or Canvas coordinates.
Essentially, you get the coordinates of last draw as a [Vector](#Vector)."}
                    {:name ".getCamSize"
                     :arglists '([tiled-map])
                     :doc "### Returns

The camera's width and height as a [Vector](#Vector)"}
                    {:name ".setCamSize"
                     :arglists '([tiled-map width height])
                     :doc "Only useful for some pre-draw calculations, since Cam
size is always the last Canvas used to draw.

### Parameters

* `width`  cam width
* `height`  cam height"}])

(def tiled-map-fns (vec (concat tiled-map-fns
                          [{:name ".getDrawMargin"
                            :arglists '([tiled-map])
                            :doc "### Returns

Number of tiles to be draw in excess around Canvas. Default: 2"}
                           {:name ".setDrawMargin"
                            :arglists '([tiled-map margin])
                            :doc "### Parameters

* `margin`  Number of tiles to be draw in excess around Canvas. Default: 2"}
                           {:name ".getDrawMode"
                            :arglists '([tiled-map])
                            :doc "### Returns

CORNER or CENTER. Default: CORNER"}
                           {:name ".setDrawMode"
                            :arglists '([tiled-map mode])
                            :doc "Defines the meaning of draw and drawLayer coordinates.
Traditionally, they are camLeft and camTop - the left/top coordinates of the camera,
but with setDrawMode(CENTER), they become the coordinates of the camera center.

### Parameters

* `mode`  CORNER or CENTER"}
                           {:name ".getPositionMode"
                            :arglists '([tiled-map])
                            :doc "### Returns

\"CANVAS\" or \"MAP\". Default: \"CANVAS\""}
                           {:name ".setPositionMode"
                            :arglists '([tiled-map mode])
                            :doc "Defines the meaning of draw and drawLayer coordinates.
Traditionally, the coordinates are read as pixel position.
but with setPositionMode(\"MAP\"), they become Tiles Coordinates.

### Parameters

* `mode`  \"CANVAS\" or \"MAP\""}
                           {:name ".getType"
                            :arglists '([tiled-map layer])
                            :doc "### Parameters

* `layer`  layer index

### Returns

Layer type (\"tilelayer\", \"imagelayer\" or \"objectgroup\")"}])))

(def tiled-map-fns (vec (concat tiled-map-fns
                          [{:name ".getVisible"
                            :arglists '([tiled-map layer])
                            :doc "### Parameters

* `layer`  layer index

### Returns

Layer visibility."}
                           {:name ".setVisible"
                            :arglists '([tiled-map layer visible])
                            :doc "### Parameters

* `layer`  layer index
* `visible`  true or false"}
                           {:name ".getImage"
                            :arglists '([tiled-map layer])
                            :doc "### Parameters

* `layer`  layer index

### Returns

[Image](#Image) of an Image Layer."}
                           {:name ".getObjects"
                            :arglists '([tiled-map layer])
                            :doc "### Parameters

* `layer`  layer index

### Returns

Array of Objects on a Object Layer."}
                           {:name ".getObjectsColor"
                            :arglists '([tiled-map layer])
                            :doc "### Parameters

* `layer`  layer index

### Returns

[Color](#Color) of an Object Layer. Default: Black."}
                           {:name ".getData"
                            :arglists '([tiled-map layer])
                            :doc "### Parameters

* `layer`  layer index

### Returns

Data of a Tile Layer."}
                           {:name ".getCustomProperties"
                            :arglists '([tiled-map layer])
                            :doc "### Parameters

* `layer`  layer index

### Returns

Custom Properties of a Layer."}])))

(def tiled-map-fns (vec (concat tiled-map-fns
                          [{:name ".getOpacity"
                            :arglists '([tiled-map layer])
                            :doc "### Parameters

* `layer`  layer index

### Returns

Opacity of a Layer (between 0 and 1)."}
                           {:name ".setOpacity"
                            :arglists '([tiled-map layer opacity])
                            :doc "### Parameters

* `layer`  layer index
* `opacity`  Opacity of a Layer (between 0 and 1)."}
                           {:name ".getTileIndex"
                            :arglists '([tiled-map layer x y])
                            :doc "Returns the tile index. Remember that:
- x and y are Integers.
- 0 is an empty tile.
- The stored tile index is the index indicated by Tiled +1.

### Parameters

* `layer`  layer index
* `x`  horizontal coordinate
* `y`  vertical coordinate

### Returns

Tile index."}
                           {:name ".setTileIndex"
                            :arglists '([tiled-map layer x y t])
                            :doc "Changes a tile index. Remember that:
- x and y are Integers.
- 0 is an empty tile.
- The stored tile index is the index indicated by Tiled +1.

### Parameters

* `layer`  layer index
* `x`  horizontal coordinate
* `y`  vertical coordinate
* `t` tile index"}])))

(def tiled-map-fns (vec (concat tiled-map-fns
                          [{:name ".canvasToMap"
                            :arglists '([tiled-map x y])
                            :doc "### Parameters

* `x`  canvas horizontal coordinate
* `y`  canvas vertical coordinate

### Returns

Map coordinates as a [Vector](#Vector)."}
                           {:name ".mapToCanvas"
                            :arglists '([tiled-map x y])
                            :doc "### Parameters

* `x`  map horizontal coordinate
* `y`  map vertical coordinate

### Returns

Canvas coordinates as a [Vector](#Vector)."}
                           {:name ".camToCanvas"
                            :arglists '([tiled-map x y])
                            :doc "### Parameters

* `x`  cam horizontal coordinate
* `y`  cam vertical coordinate

### Returns

Canvas coordinates as a [Vector](#Vector)."}
                           {:name ".canvasToCam"
                            :arglists '([tiled-map x y])
                            :doc "### Parameters

* `x`  canvas horizontal coordinate
* `y`  canvas vertical coordinate

### Returns

Cam coordinates as a [Vector](#Vector)."}
                           {:name ".camToMap"
                            :arglists '([tiled-map x y])
                            :doc "### Parameters

* `x`  cam horizontal coordinate
* `y`  cam vertical coordinate

### Returns

Map coordinates as a [Vector](#Vector)."}
                           {:name ".mapToCam"
                            :arglists '([tiled-map x y])
                            :doc "### Parameters

* `x`  map horizontal coordinate
* `y`  map vertical coordinate

### Returns

Cam coordinates as a [Vector](#Vector)."}])))

; Vector

(def vect {:name "Vector"
           :doc "Vector objects are used to hold (`x`, `y`) positions. They are
returned by some of the JavaScript functions. For your own code, use ClojureScript's
built-in vectors instead. See [the p5.js docs](http://p5js.org/reference/#/p5.Vector) for more."})

(def color {:name "Color"
            :doc "Color objects represent a single given color. See
[the p5.js docs](http://p5js.org/reference/#/p5/color) for more."})

; HTML

(defn description->str [{:keys [name arglists doc]}]
  (list
    [:a {:name (str/replace (str name) #"\." "")}]
    (for [arglist arglists]
      [:h2 (pr-str (conj (apply list arglist) (symbol name)))])
    (md-to-html-string doc)))

(spit "doc.html"
  (html [:html
         [:body
          [:h1 "Screen"]
          (description->str screen)
          [:h1 "Game"]
          (description->str game)
          [:h1 "Functions"]
          (description->str create-game)
          (map description->str game-fns)
          [:h1 "Elements"]
          [:h2 "Coming soon..."]
          [:h1 "Image"]
          (description->str image)
          (map description->str image-fns)
          [:h1 "TiledMap"]
          (description->str tiled-map)
          (map description->str tiled-map-fns)
          [:h1 "Vector"]
          (description->str vect)
          [:h1 "Color"]
          (description->str color)]]))

