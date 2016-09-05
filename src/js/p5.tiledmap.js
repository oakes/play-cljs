(function (root, factory) {
  if (typeof define === 'function' && define.amd)
    define('p5.tiledmap', ['p5'], function (p5) { (factory(p5));});
  else if (typeof exports === 'object')
    factory(require('../p5'));
  else
    factory(root['p5']);
}(this, function (p5) {

// =============================================================================
//                         p5.tiledmap Library
// =============================================================================

/**
 *  p5.tiledmap
 *  Add Tiled Maps to your sketch.
 *  What's not working (and maybe never will):
 *    Loading TMX files - p5.tiledMap uses Tiled JavaScript export, which must be loaded previously.
 *    External TileSets - must be imported to be part of JavaScript export.
 *    Opacity (bug #1311 [https://github.com/processing/p5.js/issues/1311]).
 *    Tiles renderorder is not considered - always renders "right-down".
 *    Objects draworder is not considered.
 *  It's advisable to export Tiled maps to the same folder - pay attention to images path on js!
 *  The p5.tiledmap.js library is free software; you can redistribute it and/or modify it under the terms
 *  of the GNU Lesser General Public License as published by the Free Software Foundation, version 2.1.
 *  Last p5.tiledmap available on https://github.com/linux-man/p5.tiledmap
 */

p5.TiledMap = function(renderer, mapname, imagepath, transparentoffset) {
  'use strict';
  this.renderer = renderer;
  this.name = mapname;
  this.path = imagepath;
  this.drawmode = this.renderer.CORNER;
  this.positionmode = "CANVAS";
  this.transparentoffset = transparentoffset;
  this.drawmargin = 2;
  this.tile = [];
  this.layerimage = [];
  this.orientation = TileMaps[this.name].orientation;
  this.tilewidth = TileMaps[this.name].tilewidth;
  this.tileheight = TileMaps[this.name].tileheight;
  this.mapwidth = TileMaps[this.name].width;
  this.mapheight = TileMaps[this.name].height;
  this.camleft = 0;
  this.camtop = 0;
  this.camwidth = parent.width;
  this.camheight = parent.height;
  this.staggeraxis = TileMaps[this.name].staggeraxis || "x";
  this.staggerindex = Number(TileMaps[this.name].staggerindex == "even");
  this.hexsidelength = TileMaps[this.name].hexsidelength || 0;
  // Rewrite drawTileLayer, mapToCanvas and canvasToMap for the several geometries
  switch(TileMaps[this.name].orientation) {
    case "isometric":
      drawTileLayer = function(layer, pg) {
        'use strict';
        var n, x, y, p, tileN;
        var offsetx = layer.offsetx || 0;
        var offsety = layer.offsety || 0;
        var xstart = Math.max(0, Math.floor(this.canvasToMap(this.camleft, this.camtop).x - this.drawmargin));
        var xstop = Math.min(this.mapwidth, Math.ceil(this.canvasToMap(this.camleft + pg.width, this.camtop + pg.height).x + this.drawmargin));
        var ystart = Math.max(0, Math.floor(this.canvasToMap(this.camleft + pg.width, this.camtop).y - this.drawmargin));
        var ystop = Math.min(this.mapheight, Math.ceil(this.canvasToMap(this.camleft, this.camtop + pg.height).y + this.drawmargin));
        for (var ny = ystart; ny < ystop; ny++) for (var nx = xstart; nx < xstop; nx++) {
          n = layer.data[nx + ny * this.mapwidth];
          if (this.tile[n]) tileN = this.tile[n];
          else tileN = this.tile[0];
          p = this.mapToCam(nx, ny);
          x = p.x - this.tilewidth / 2 + tileN.offsetx + offsetx;
          y = p.y - this.tileheight / 2 + tileN.offsety + offsety + (this.tileheight - tileN.image.height);;
          pg.image(tileN.image, x, y);
        }
      }
      mapToCanvas = function(p) {
        return this.renderer.createVector((this.mapwidth + this.mapheight) * this.tilewidth / 4 + (p.x - p.y) * this.tilewidth / 2, (p.x + p.y + 1) * this.tileheight / 2);
      }
      canvasToMap = function(p) {
        var dif = (this.mapwidth + this.mapheight) * this.tilewidth / 4;
        return this.renderer.createVector(p.y / this.tileheight + ((p.x - dif) / this.tilewidth) - 0.5, p.y / this.tileheight - ((p.x - dif) / this.tilewidth) - 0.5);
      }
      break;
      case "staggered":
        this.hexsidelength = 0;
      case "hexagonal":
        switch(this.staggeraxis) {
          case("x"):
            drawTileLayer = function(layer, pg) {
              'use strict';
              var n, x, y, p, tileN;
              var offsetx = layer.offsetx || 0;
              var offsety = layer.offsety || 0;
              var xstart = Math.floor(this.canvasToMap(this.camleft, this.camtop).x - this.drawmargin)
              xstart = Math.max(0, xstart - xstart % 2);
              var xstop = Math.min(this.mapwidth, Math.ceil(this.canvasToMap(this.camleft + pg.width, this.camtop + pg.height).x + this.drawmargin));
              var ystart = Math.max(0, Math.floor(this.canvasToMap(this.camleft, this.camtop).y - this.drawmargin));
              var ystop = Math.min(this.mapheight, Math.ceil(this.canvasToMap(this.camleft + pg.width, this.camtop + pg.height).y + this.drawmargin));
              for (var ny = ystart; ny < ystop; ny++) for (var nx = xstart; nx <= xstop; nx+=2) for (var nz = this.staggerindex; nz >= this.staggerindex - 1; nz--) {
                if (nx + nz >= 0 && nx + nz < this.mapwidth) {
                  n = layer.data[nx + nz + ny * this.mapwidth];
                  if (this.tile[n]) tileN = this.tile[n];
                  else tileN = this.tile[0];
                  p = this.mapToCam(nx + nz, ny);
                  x = p.x - this.tilewidth / 2 + tileN.offsetx + offsetx;
                  y = p.y - this.tileheight / 2 + tileN.offsety + offsety + (this.tileheight - tileN.image.height);
                  pg.image(tileN.image, x, y);
                }
              }
            }
            mapToCanvas = function(p) {
              var x = (p.x * (this.hexsidelength + this.tilewidth) + this.tilewidth) / 2;
              var y = (p.y + 0.5 + this.staggerindex / 2) * this.tileheight + (this.staggerindex * 2 - 1) * (Math.abs(Math.abs(p.x) % 2 - 1) - 1) * (this.tileheight) / 2;
              return this.renderer.createVector(x, y);
            }
            canvasToMap = function(p) {
              var ny;
              var nx = (p.x * 2 - this.tilewidth) / (this.hexsidelength + this.tilewidth);
              if (this.staggerindex == 0) ny = p.y / this.tileheight - 1 + (Math.abs(Math.abs(nx) % 2 - 1)) / 2;
              else ny = p.y / this.tileheight - 0.5 - (Math.abs(Math.abs(nx) % 2 - 1)) / 2;
              return this.renderer.createVector(nx, ny);
            }
            break;
          case("y"):
            drawTileLayer = function(layer, pg) {
              'use strict';
              var n, x, y, p, tileN;
              var offsetx = layer.offsetx || 0;
              var offsety = layer.offsety || 0;
              var xstart = Math.max(0, Math.floor(this.canvasToMap(this.camleft, this.camtop).x - this.drawmargin));
              var xstop = Math.min(this.mapwidth, Math.ceil(this.canvasToMap(this.camleft + pg.width, this.camtop + pg.height).x + this.drawmargin));
              var ystart = Math.max(0, Math.floor(this.canvasToMap(this.camleft, this.camtop).y - this.drawmargin));
              var ystop = Math.min(this.mapheight, Math.ceil(this.canvasToMap(this.camleft + pg.width, this.camtop + pg.height).y + this.drawmargin));
              for (var ny = ystart; ny < ystop; ny++) for (var nx = xstart; nx < xstop; nx++) {
                n = layer.data[nx + ny * this.mapwidth];
                if (this.tile[n]) tileN = this.tile[n];
                else tileN = this.tile[0];
                p = this.mapToCam(nx, ny);
                x = p.x - this.tilewidth / 2 + tileN.offsetx + offsetx;
                y = p.y - this.tileheight / 2 + tileN.offsety + offsety + (this.tileheight - tileN.image.height);;
                pg.image(tileN.image, x, y);
              }
            }
            mapToCanvas = function(p) {
              var y = p.y * (this.hexsidelength + this.tileheight) / 2 + this.tileheight / 2;
              var x = (p.x + 0.5 + this.staggerindex / 2) * this.tilewidth + (this.staggerindex * 2 - 1) * (Math.abs(Math.abs(p.y) % 2 - 1) - 1) * (this.tilewidth) / 2;
              return this.renderer.createVector(x, y);
            }
            canvasToMap = function(p) {
              var nx;
              var ny = (p.y * 2 - this.tileheight) / (this.hexsidelength + this.tileheight);
              if (this.staggerindex == 0) nx = p.x / this.tilewidth - 1 + (Math.abs(Math.abs(ny) % 2 - 1)) / 2;
              else nx = p.x / this.tilewidth - 0.5 - (Math.abs(Math.abs(ny) % 2 - 1)) / 2;
              return this.renderer.createVector(nx, ny);
            }
            break;
        }
      break;
    default: // drawTileLayer for Orthogonal geometry (default)
      drawTileLayer = function(layer, pg) {
        'use strict';
        var n, x, y, p, tileN;
        var offsetx = layer.offsetx || 0;
        var offsety = layer.offsety || 0;
        var xstart = Math.max(0, Math.floor(this.canvasToMap(this.camleft, this.camtop).x - this.drawmargin));
        var xstop = Math.min(this.mapwidth, Math.ceil(this.canvasToMap(this.camleft + pg.width, this.camtop + pg.height).x + this.drawmargin));
        var ystart = Math.max(0, Math.floor(this.canvasToMap(this.camleft, this.camtop).y - this.drawmargin));
        var ystop = Math.min(this.mapheight, Math.ceil(this.canvasToMap(this.camleft + pg.width, this.camtop + pg.height).y + this.drawmargin));
        for (var ny = ystart; ny < ystop; ny++) for (var nx = xstart; nx < xstop; nx++) {
          n = layer.data[nx + ny * this.mapwidth];
          if (this.tile[n]) tileN = this.tile[n];
          else tileN = this.tile[0];
          p = this.mapToCam(nx, ny);
          x = p.x - this.tilewidth / 2 + tileN.offsetx + offsetx;
          y = p.y - this.tileheight / 2 + tileN.offsety + offsety + (this.tileheight - tileN.image.height);;
          pg.image(tileN.image, x, y);
        }
      }
      mapToCanvas = function(p) {
        return this.renderer.createVector((p.x + 0.5) * this.tilewidth, (p.y + 0.5) * this.tileheight)
      }
      canvasToMap = function(p) {
        return this.renderer.createVector(p.x / this.tilewidth - 0.5, p.y / this.tileheight - 0.5)
      }
  }
  for (var n = 0; n < TileMaps[this.name].layers.length; n++) {
    var layer = TileMaps[this.name].layers[n];
    if (layer.type == "imagelayer") this.layerimage[layer.image] = this.renderer.loadImage(this.path + layer.image);
  }
  this.tile[0] = new Tile(this.renderer.createImage(1,1), 0, 0);
  cicleTilesets.call(this, 0);
}

// =============================================================================
//                         Private Functions
// =============================================================================

Tile = function(image, offsetx, offsety) {
  'use strict';
  this.image = image;
  this.offsetx = offsetx;
  this.offsety = offsety;
}

function cicleTilesets(n) {
  'use strict';
  if (n < TileMaps[this.name].tilesets.length) {
    var tileset = TileMaps[this.name].tilesets[n];
    if (tileset.image){
      this.renderer.loadImage(this.path + tileset.image, (function(img) {loadTiles.call(this, img, n)}).bind(this));
    }
    else if (tileset.tiles) {
      var firstgid = tileset.firstgid;
      if (tileset.tileoffset) {
        var tileoffsetx = tileset.tileoffset.x || 0;
        var tileoffsety = tileset.tileoffset.y || 0;
      }
      else {
        var tileoffsetx = 0;
        var tileoffsety = 0;
      }
      for (var k in tileset.tiles) {
        var img = this.renderer.loadImage(this.path + tileset.tiles[k].image);
        this.tile[firstgid + int(k)] = new Tile(img, tileoffsetx, tileoffsety);
      }
    }
  }
}

function loadTiles(imgsrc, n) {
  'use strict';
  var tileset = TileMaps[this.name].tilesets[n];
  var firstgid = tileset.firstgid;
  var tilewidth = tileset.tilewidth;
  var tileheight = tileset.tileheight;
  var tilecount = tileset.tilecount || 0;
  var columns = tileset.columns || 0;
  var spacing = tileset.spacing || 0;
  var margin = tileset.margin || 0;
  if (tileset.tileoffset) {
    var tileoffsetx = tileset.tileoffset.x || 0;
    var tileoffsety = tileset.tileoffset.y || 0;
  }
  else {
    var tileoffsetx = 0;
    var tileoffsety = 0;
  }
  if (columns == 0) columns = Math.floor((imgsrc.width - margin) / (tilewidth + spacing));
  if (tilecount == 0) tilecount = columns * Math.floor((imgsrc.height - margin) / (tileheight + spacing));
  if (tileset.transparentcolor) applyTransColor(this.renderer, imgsrc, this.renderer.color(tileset.transparentcolor), this.transparentoffset);
  for (var m = 0; m < tilecount; m++) {
    var img = imgsrc.get(margin + (m % columns) * (tilewidth + spacing), margin + Math.floor(m / columns) * (tileheight + spacing), tilewidth, tileheight);
    this.tile[firstgid + m] = new Tile(img, tileoffsetx, tileoffsety);
  }
  cicleTilesets.call(this, ++n);
}

function applyTransColor(renderer, img, trans, offset) {
  'use strict';
  img.loadPixels();
  for (var p = 0; p < img.pixels.length; p+=4) {
    if (Math.abs(img.pixels[p] - renderer.red(trans)) < offset
    && Math.abs(img.pixels[p+1] - renderer.green(trans)) < offset
    && Math.abs(img.pixels[p+2] == renderer.blue(trans)) < offset) {
      img.pixels[p+3] = 0;
    }
  }
  img.updatePixels();
}

// Functions skeleton
drawTileLayer = function(layer, pg) {}

mapToCanvas = function(p) {}

canvasToMap = function(p) {}

// =============================================================================
//                         Public Methods
// =============================================================================

p5.prototype.registerPreloadMethod('loadTiledMap', p5.prototype);

/**
 *  loadTiledMap() returns a new p5.TiledMap.
 *  TiledMap uses Tiled JavaScript export, which must be loaded previously:
 *  <script language="javascript" type="text/javascript" src="data/desert.js"></script>
 *  External TileSets are not supported, just because they are not part of the script.
 *  If called during preload(), the p5.TiledMap will be ready to play in time for setup()
 *  and draw().
 *  If called outside of preload, the p5.TiledMap will not be ready immediately,
 *  so loadTiledMap accepts a callback as the last parameter.
 *
 *  @method loadTiledMap
 *  @param  {String}   mapName               Map name.
 *  @param  {String}   [imagePath]           Path to the image file(s). Default: "/".
 *  @param  {Number}   [transparentOffset]   Maximum difference on RGB channels to apply Tile
 *                                           transparency. Default: 4.
 *  @param  {Function} [callback]            Name of a function to call once map loads.
 *  @return {p5.TiledMap}
 *  @example
 *  <div><code>
 *  function preload() {
 *   tmap = loadTiledMap("desert", "data");
 *  }
 *
 *  function setup() {
 *    createCanvas(800, 600);
 *  }
 *  function draw() {
 *    background(128);
 *    tmap.draw(0, 0);
 *  }
 *  </code></div>
 */

p5.prototype.loadTiledMap = function () {
  'use strict';
  var mapname = arguments[0];
  var imagepath = "";
  var transparentoffset = 4;
  var callback = null;
  for (var i = 1; i < arguments.length; i++) {
    if (typeof(arguments[i]) === 'string') imagepath = arguments[i];
    if (typeof(arguments[i]) === 'number') transparentoffset = arguments[i];
    if (typeof(arguments[i]) === 'function') callback = arguments[i];
  }
  if (imagepath.length > 0 && imagepath[-1] != "/") imagepath = imagepath + "/";
  if (!TileMaps) throw "No Tiled Map found!";
  if (!TileMaps[mapname]) throw "No Tiled Map named "+mapname+" found!";
  var t =  new p5.TiledMap(this, mapname, imagepath, transparentoffset);
  if (typeof(callback) === 'function') callback(t);
  return t;
}

/**
 *  drawLayer() draws a TiledMap Layer.
 *  Visible property is ignored when drawing individual layers.
 *  The use of a off-screen graphics buffer is optional. Can be used for extra image editing or
 *  if you want to use the layer opacity property (which is ignored when drawing to canvas).
 *  Canvas (or the off-screen graphics buffer) IS NOT CLEARED BEFORE (that's up to you).
 *  That means you can draw several layers on a buffer, but the layer opacity will affect all
 *  layers drawn before.
 *  Opacity not working until bug #1311 [https://github.com/processing/p5.js/issues/1311]
 *  is fixed.
 *
 *  @method drawLayer
 *  @param  {Number}   layer                 Layer Index.
 *  @param  {Number}   camLeft               Left Coordinate.
 *  @param  {Number}   camTop                Top Coordinate.
 *  @param  {Object}   [pg]                  off-screen graphics buffer.
 *  @example
 *  <div><code>
 *  function preload() {
 *   tmap = loadTiledMap("desert", "data");
 *  }
 *
 *  function setup() {
 *    createCanvas(800, 600);
 *    pg = createGraphics(800, 600);
 *  }
 *  function draw() {
 *    pg.clear();
 *    tmap.drawLayer(0, 0, 0, pg);
 *    image(pg, 0, 0);
 *  }
 *  </code></div>
 */

p5.TiledMap.prototype.drawLayer = function(n, x, y, pg) {
  'use strict';
  if (!pg) pg = this.renderer;
  this.camwidth = pg.width;
  this.camheight = pg.height;
  if (this.positionmode == "MAP") {
    var pos = this.mapToCanvas(x, y);
    x = pos.x;
    y = pos.y;
  }
  if (this.drawmode == this.renderer.CENTER) {
    this.camleft = x - this.camwidth / 2;
    this.camtop = y - this.camheight / 2;
  }
  else{
    this.camleft = x;
    this.camtop = y;
  }
  pg.push();
  pg.resetMatrix();
  pg.ellipseMode(this.renderer.CORNER);
  pg.angleMode(this.renderer.DEGREES);
  pg.imageMode(this.renderer.CORNER);
  var layer = TileMaps[this.name].layers[n];
  switch (layer.type) {
    case "tilelayer":
      drawTileLayer.call(this, layer, pg);
      break;
    case "imagelayer":
      var offsetx = layer.offsetx || 0;
      var offsety = layer.offsety || 0;
      pg.image(this.layerimage[layer.image], -this.camleft + offsetx, -this.camtop + offsety);
      break;
    case "objectgroup":
      var offsetx = layer.offsetx || 0;
      var offsety = layer.offsety || 0;
      pg.fill(this.renderer.color(layer.color || 0));
      pg.stroke(this.renderer.color(layer.color || 0));
      pg.strokeWeight(1);
      for (var n in layer.objects) {
        var o = layer.objects[n];
        if (o.visible) {
          pg.push();
          pg.resetMatrix();
          pg.translate(o.x - offsetx - this.camleft, o.y - offsety - this.camtop);
          if (o.rotation) pg.rotate(o.rotation);
          if (o.ellipse) pg.ellipse(0, 0, o.width, o.height);
          else if (o.polyline) {
            pg.noFill();
            pg.beginShape();
            for (var p in o.polyline) pg.vertex(o.polyline[p].x, o.polyline[p].y);
            pg.endShape();
          }
          else if (o.polygon) {
            pg.beginShape();
            for (var p in o.polygon) pg.vertex(o.polygon[p].x, o.polygon[p].y);
            pg.endShape(CLOSE);
          }
          else if (o.gid) {
            if (o.rotation) pg.rotate(-o.rotation);
            pg.translate(0, -this.tile[o.gid].image.height);
            if (o.rotation) pg.rotate(o.rotation);
            pg.image(this.tile[o.gid].image, this.tile[o.gid].offsetx, this.tile[o.gid].offsety, o.width, o.height);
          }
          else pg.rect(0, 0, o.width, o.height);
          pg.pop();
        }
      }
      break;
  }
  if ((pg !== parent) && (layer.opacity < 1)) { //applyOpacity
    pg.loadPixels();
    var op = map(layer.opacity, 0, 1, 0, 255);
    for (var p = 3; p < pg.pixels.length; p+=4) if (pg.pixels[p] > op) pg.pixels[p] = op;
    pg.updatePixels();
  }
  pg.pop();
}

/**
 *  draw() draws a TiledMap.
 *  Visible property is respected.
 *  Opacity is ignored.
 *  The use of a off-screen graphics buffer is optional.
 *  Canvas (or the off-screen graphics buffer) IS NOT CLEARED BEFORE.
 *
 *  @method draw
 *  @param  {Number}   camLeft               Left Coordinate.
 *  @param  {Number}   camTop                Top Coordinate.
 *  @param  {Object}   [pg]                  off-screen graphics buffer.
 *  @example
 *  <div><code>
 *  function preload() {
 *   tmap = loadTiledMap("desert", "data");
 *  }
 *
 *  function setup() {
 *    createCanvas(800, 600);
 *  }
 *  function draw() {
 *    background(128);
 *    tmap.draw(0, 0);
 *  }
 *  </code></div>
 */

p5.TiledMap.prototype.draw = function(camleft, camtop, pg) {
  var o;
  for (var n = 0; n < TileMaps[this.name].layers.length; n++)
    if (TileMaps[this.name].layers[n].visible) {
      o = TileMaps[this.name].layers[n].opacity;
      TileMaps[this.name].layers[n].opacity = 1;
      this.drawLayer(n, camleft, camtop, pg);
      TileMaps[this.name].layers[n].opacity = o;
    }
}

/**
 *  Maps are stored and accessible in TiledMaps Object.
 *  These are helper methods to ease Map and Layer management.
 */

 /**
  *  @method getName
  *  @return {String}                       Map Name
  */

 p5.TiledMap.prototype.getName = function() {
   'use strict';
   return this.name;
 }

/**
 *  @method getVersion
 *  @return {Number}                        Map Version
 */

p5.TiledMap.prototype.getVersion = function() {
  'use strict';
  return TileMaps[this.name].version;
}

/**
 *  @method getVersion
 *  @return {String}                        Map Orientation
 */

p5.TiledMap.prototype.getOrientation = function() {
  'use strict';
  return TileMaps[this.name].orientation;
}

 /**
  *  @method getBackgroundColor
  *  @return {Array}                         P5 Color
  */

p5.TiledMap.prototype.getBackgroundColor = function() {
  'use strict';
  if (TileMaps[this.name].backgroundcolor)
    return this.renderer.color(TileMaps[this.name].backgroundcolor);
  else return this.renderer.color(255);
}

/**
 *  @method getMapSize
 *  @return {p5.Vector}                     Map Width and Height (in number of tiles)
 */

p5.TiledMap.prototype.getMapSize = function() {
  return this.renderer.createVector(this.mapwidth, this.mapheight);
}

/**
 *  @method getTileSize
 *  @return {p5.Vector}                     Tile Width and Height (in pixels)
 */

p5.TiledMap.prototype.getTileSize = function() {
  return this.renderer.createVector(this.tilewidth, this.tileheight);
}

/**
 *  @method getHexSideLength
 *  @return {Number}                        Only for Hexagonal Maps
 */

p5.TiledMap.prototype.getHexSideLength = function() {
  return this.hexsidelength;
}

/**
 *  @method getCamCorner
 *  @return {p5.Vector}                     Left Top Corner Coordinates
 */

p5.TiledMap.prototype.getCamCorner = function() {
  if (this.positionmode == "MAP")
    return this.canvasToMap(this.camleft, this.camtop);
  else return this.renderer.createVector(this.camleft, this.camtop);
}

/**
 *  @method getCamCenter
 *  @return {p5.Vector}                     Center Coordinates
 */

p5.TiledMap.prototype.getCamCenter = function() {
  if (this.positionmode == "MAP")
    return this.canvasToMap(this.camleft + this.camwidth / 2, this.camtop + this.camheight / 2);
  else return this.renderer.createVector(this.camleft + this.camwidth / 2, this.camtop + this.camheight / 2);
}

/**
 *  Depending on dramMode, returns the camera corner or center coordinates.
 *  Depending on positionMode, returns the Map or Canvas coordinates.
 *  Essentially, you get the coordinates of last draw.
 *
 *  @method getPosition
 *  @return {p5.Vector}
 */

p5.TiledMap.prototype.getPosition = function() {
  if (this.drawmode == parent.CORNER) return this.getCamCorner();
  else return this.getCamCenter();
}

/**
 *  @method getCamSize
 *  @return {p5.Vector}                     Camera (last used Canvas) Width and Height
 */

p5.TiledMap.prototype.getCamSize = function() {
  return this.renderer.createVector(this.camwidth, this.camheight);
}

/**
 *  Only useful for some pre-draw calculations, since Cam size is always the last Canvas used to draw.
 *
 *  @method setCamSize
 *  @param  {P5.Vector}   (width, height)
 */
/**
*  Only useful for some pre-draw calculations, since Cam size is always the last Canvas used to draw.
*
 *  @param  {Number}   width
 *  @param  {Number}   height
 */

p5.TiledMap.prototype.setCamSize = function(a, b) {
  if (typeof(a) === "object") {
    this.camwidth = a.x;
    this.camheight = a.y;
  }
  else {
    this.camwidth = a;
    this.camheight = b;
  }
}

/**
 *  @method getDrawMargin
 *  @return {Number}   margin               Number of tiles to be draw in excess around Canvas. Default: 2.
 */

p5.TiledMap.prototype.getDrawMargin = function() {
  return this.drawmargin;
}

  /**
   *  @method setDrawMargin
   *  @param {Number}  margin               Number of tiles to be draw in excess around Canvas. Default: 2.
   */

p5.TiledMap.prototype.setDrawMargin = function(n) {
  this.drawmargin = n;
}

/**
 *  @method getDrawMode
 *  @return {Constant} mode                 CORNER or CENTER. Default: CORNER.
 */

p5.TiledMap.prototype.getDrawMode = function() {
  return this.drawmode;
}

/**
 *  Defines the meaning of draw and drawLayer coordinates.
 *  Tradicionally, they are camLeft and camTop - the left/top coordinates of the camera,
 *  but with setDrawMode(CENTER), they become the coordinates of the camera center.
 *
 *  @method setDrawMode
 *  @param  {Constant} mode                 CORNER or CENTER
 */

p5.TiledMap.prototype.setDrawMode = function(s) {
  if (s == parent.CORNER || s == parent.CENTER) this.drawmode = s;
}

/**
 *  @method getPositionMode
 *  @return {String}   mode                 "CANVAS" or "MAP". Default: "CANVAS".
 */

p5.TiledMap.prototype.getPositionMode = function() {
  return this.positionmode;
}

/**
 *  Defines the meaning of draw and drawLayer coordinates.
 *  Tradicionally, the coordinates are read as pixel position.
 *  but with setPositionMode("MAP"), they become Tiles Coordinates.
 *
 *  @method setPositionMode
 *  @param  {String}   mode                 "CANVAS" or "MAP".
 */

p5.TiledMap.prototype.setPositionMode = function(s) {
  if (s == "CANVAS" || s == "MAP") this.positionmode = s;
}

/**
 *  @method getType
 *  @param  {Number}   layer                 Layer Index.
 *  @return {String}                         Returns Layer Type ("tilelayer", "imagelayer" or "objectgroup").
 */

p5.TiledMap.prototype.getType = function(n) {
  'use strict';
  return TileMaps[this.name].layers[n].type;
}

/**
 *  @method getVisible
 *  @param  {Number}   layer                 Layer Index.
 *  @return {Boolean}                        Returns Layer Visibility.
 */

p5.TiledMap.prototype.getVisible = function(n) {
  'use strict';
  return TileMaps[this.name].layers[n].visible;
}

/**
 *  @method setVisible
 *  @param  {Number}   layer                 Layer Index.
 *  @param  {Boolean}  visible               Visible value.
 */

p5.TiledMap.prototype.setVisible = function(n, v) {
  'use strict';
  TileMaps[this.name].layers[n].visible = v;
}

/**
 *  @method getImage
 *  @param  {Number}   layer                 Layer Index.
 *  @return {p5.Image}                       Image of a Image Layer.
 */

p5.TiledMap.prototype.getImage = function(n) {
  'use strict';
  return this.layerimage[TileMaps[this.name].layers[n].image];
}

/**
 *  @method getObjects
 *  @param  {Number}   layer                 Layer Index.
 *  @return {Array}                          Array of Objects on a Object Layer.
 */

p5.TiledMap.prototype.getObjects = function(n) {
  'use strict';
  return TileMaps[this.name].layers[n].objects;
}

/**
 *  @method getObjectsColor
 *  @param  {Number}   layer                 Layer Index.
 *  @return {Array}                          P5 Color of an Object Layer. Default: Black.
 */

p5.TiledMap.prototype.getObjectsColor = function(n) {
  'use strict';
  return this.renderer.color(TileMaps[this.name].layers[n].color || 0);
}

/**
 *  @method getData
 *  @param  {Number}   layer                 Layer Index.
 *  @return {Array}                          Data of a Tile Layer.
 */

p5.TiledMap.prototype.getData = function(n) {
  'use strict';
  return TileMaps[this.name].layers[n].data;
}

/**
 *  @method getCustomProperties
 *  @param  {Number}   layer                 Layer Index.
 *  @return {Object}                         Custom Properties of a Layer.
 */

p5.TiledMap.prototype.getCustomProperties = function(n) {
  'use strict';
  return TileMaps[this.name].layers[n].properties;
}

/**
 *  @method getOpacity
 *  @param  {Number}   layer                 Layer Index.
 *  @return {Number}                         Opacity of a Layer (between 0 and 1).
 */

p5.TiledMap.prototype.getOpacity = function(n) {
  'use strict';
  return TileMaps[this.name].layers[n].opacity;
}

/**
 *  @method setOpacity
 *  @param  {Number}   layer                 Layer Index.
 *  @param  {Number}   opacity               Opacity of the Layer (between 0 and 1).
 */

p5.TiledMap.prototype.setOpacity = function(n, o) {
  'use strict';
  TileMaps[this.name].layers[n].opacity = Math.min(Math.max(0, o), 1);
}

/**
 *  getTileIndex return the tile index. Remember that:
 *  - x and y are Integers.
 *  - 0 is an empty tile.
 *  - The stored tile index is the index indicated by Tiled +1.
 *
 *  @method getTileIndex
 *  @param  {Number}   layer                 Layer Index.
 *  @param  {Number}   x                     Horizontal Coordinate.
 *  @param  {Number}   y                     Vertical Coordinate.
 *  @return {Number}                         Tile Index.
 */
/**
 *  getTileIndex return the tile index. Remember that:
 *  - x and y are Integers.
 *  - 0 is an empty tile.
 *  - The stored tile index is the index indicated by Tiled +1.
 *
 *  @param  {Number}   layer                 Layer Index.
 *  @param  {p5.Vector}   v                  Coordinates.
 *  @return {Number}                         Tile Index.
 */

p5.TiledMap.prototype.getTileIndex = function(n, a, b) {
  'use strict';
  var x, y;
  if (typeof(a) === "object") {x = a.x; y = a.y;}
  else {x = a; y = b;}
  if (TileMaps[this.name].layers[n].type == "tilelayer" && x >= 0 && y >= 0 && x < this.mapwidth && y < this.mapheight)
    return TileMaps[this.name].layers[n].data[x + y * this.mapwidth];
  else return undefined;
}

/**
 *  setTileIndex changes a tile index. Remember that:
 *  - x, y and t are Integers.
 *  - 0 is an empty tile.
 *  - The stored tile index is the index presented by Tiled +1.
 *
 *  @method setTileIndex
 *  @param  {Number}   layer                 Layer Index.
 *  @param  {Number}   x                     Horizontal Coordinate.
 *  @param  {Number}   y                     Vertical Coordinate.
 *  @param  {Number}   t                     Tile Index.
 */
/**
 *  setTileIndex changes a tile index. Remember that:
 *  - x, y and t are Integers.
 *  - 0 is an empty tile.
 *  - The stored tile index is the index presented by Tiled +1.
 *
 *  @method setTileIndex
 *  @param  {Number}   layer                 Layer Index.
 *  @param  {p5.Vector}   v                  Coordinates.
 *  @param  {Number}   t                     Tile Index.
 */


p5.TiledMap.prototype.setTileIndex = function(n, a, b, c) {
  'use strict';
  var x, y, t;
  if (typeof(a) === "object") {x = a.x; y = a.y; t = b;}
  else {x = a; y = b; t = c;}
  if (TileMaps[this.name].layers[n].type == "tilelayer" && x >= 0 && y >= 0 && x < this.mapwidth && y < this.mapheight)
    TileMaps[this.name].layers[n].data[x + y * this.mapwidth] = t;
}

/**
 *  Convertion Methods between the 3 coordinate systems in use:
 *  MAP (Tile coordinates), CANVAS (Pixel coordinates) and CAM (relative coordinates of the draw)
 */

/**
 *  @method canvasToMap
 *  @param  {Number}   x                     Canvas Horizontal Coordinate.
 *  @param  {Number}   y                     Canvas Vertical Coordinate.
 *  @return {p5.Vector}                      Map Coordinates
 */
/**
 *  @method canvasToMap
 *  @param  {p5.Vector}   v                  Canvas Coordinates.
 *  @return {p5.Vector}                      Map Coordinates
 */
p5.TiledMap.prototype.canvasToMap = function(a, b) {
  if (typeof(a) === "number") p = this.renderer.createVector(a, b);
  else p = a;
  return canvasToMap.call(this, p);
}

/**
 *  @method mapToCanvas
 *  @param  {Number}   x                     Map Horizontal Coordinate.
 *  @param  {Number}   y                     Map Vertical Coordinate.
 *  @return {p5.Vector}                      Canvas Coordinates
 */
/**
 *  @method mapToCanvas
 *  @param  {p5.Vector}   v                  Map Coordinates.
 *  @return {p5.Vector}                      Canvas Coordinates
 */

p5.TiledMap.prototype.mapToCanvas = function(a, b) {
  var p;
  if (typeof(a) === "number") p = this.renderer.createVector(a, b);
  else p = a;
  return mapToCanvas.call(this, p);
}

/**
 *  @method camToCanvas
 *  @param  {Number}   x                     Cam Horizontal Coordinate.
 *  @param  {Number}   y                     Cam Vertical Coordinate.
 *  @return {p5.Vector}                      Canvas Coordinates
 */
/**
 *  @method camToCanvas
 *  @param  {p5.Vector}   v                  Cam Coordinates.
 *  @return {p5.Vector}                      Canvas Coordinates
 */

p5.TiledMap.prototype.camToCanvas = function(a, b) {
  var x, y;
  if (typeof(a) === "object") {x = a.x; y = a.y;}
  else {x = a; y = b;}
  return this.renderer.createVector(x + this.camleft, y + this.camtop);
}

/**
 *  @method canvasToCam
 *  @param  {Number}   x                     Canvas Horizontal Coordinate.
 *  @param  {Number}   y                     Canvas Vertical Coordinate.
 *  @return {p5.Vector}                      Cam Coordinates
 */
/**
 *  @method canvasToCam
 *  @param  {p5.Vector}   v                  Canvas Coordinates.
 *  @return {p5.Vector}                      Cam Coordinates
 */

p5.TiledMap.prototype.canvasToCam = function(a, b) {
  var x, y;
  if (typeof(a) === "object") {x = a.x; y = a.y;}
  else {x = a; y = b;}
  return this.renderer.createVector(x - this.camleft, y - this.camtop);
}

/**
 *  @method camToMap
 *  @param  {Number}   x                     Cam Horizontal Coordinate.
 *  @param  {Number}   y                     Cam Vertical Coordinate.
 *  @return {p5.Vector}                      Map Coordinates
 */
/**
 *  @method camToMap
 *  @param  {p5.Vector}   v                  Cam Coordinates.
 *  @return {p5.Vector}                      Map Coordinates
 */

p5.TiledMap.prototype.camToMap = function(a, b) {
  var x, y;
  if (typeof(a) === "object") {x = a.x; y = a.y;}
  else {x = a; y = b;}
  return this.canvasToMap(this.camToCanvas(x, y));
}

/**
 *  @method mapToCam
 *  @param  {Number}   x                     Map Horizontal Coordinate.
 *  @param  {Number}   y                     Map Vertical Coordinate.
 *  @return {p5.Vector}                      Cam Coordinates
 */
 /**
 *  @method mapToCam
 *  @param  {p5.Vector}   v                  Map Coordinates.
 *  @return {p5.Vector}                      Cam Coordinates
 */

p5.TiledMap.prototype.mapToCam = function(a, b) {
  var nx, ny;
  if (typeof(a) === "object") {nx = a.x; ny = a.y;}
  else {nx = a; ny = b;}
  return this.canvasToCam(this.mapToCanvas(nx, ny));
}

// =============================================================================
//                         The End
// =============================================================================

}));
