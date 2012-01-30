;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.keystroke
  (:use [seesaw.util :only [illegal-argument resource resource-key?]]
        seesaw.event-utils)
  (:import [javax.swing KeyStroke]
           [java.awt Toolkit AWTKeyStroke]
           [java.awt.event InputEvent])
  (:require [clojure.string :only [join split]]))

(def ^{:private true} modifier-masks {
  InputEvent/CTRL_MASK "ctrl"
  InputEvent/META_MASK "meta"
  InputEvent/ALT_MASK  "alt"
})

(defn- preprocess-descriptor [s]
  (let [mask (modifier-masks (.. (Toolkit/getDefaultToolkit) getMenuShortcutKeyMask))]
    (clojure.string/join mask (clojure.string/split s #"menu"))))

(defn- assemble-descriptor [ & args]
  "Take a keycode.  Generate key-typed.  Take modifiers.  Take up or down.")

(defn keystroke
  "Convert an argument to a KeyStroke. When the argument is a string, follows 
   the keystroke descriptor syntax for KeyStroke/getKeyStroke (see link below).
   
   For example,
   
   (keystroke \"ctrl S\")
   
   Note that there is one additional modifier supported, \"menu\" which will
   replace the modifier with the appropriate platform-specific modifier key for
   menus. For example, on Windows it will be \"ctrl\", while on OSX, it will be
   the \"command\" key. Yay!
   
   arg can also be an i18n resource keyword.
   
   See http://download.oracle.com/javase/6/docs/api/javax/swing/KeyStroke.html#getKeyStroke(java.lang.String)"
  ([arg]
  (cond 
    (nil? arg)                nil
    (instance? KeyStroke arg) arg
    (char? arg)               (KeyStroke/getKeyStroke ^Character arg)
    (integer? arg)            (KeyStroke/getKeyStroke ^Long arg 0)
    (resource-key? arg)       (keystroke (resource arg))
    (instance? InputEvent arg) (KeyStroke/getKeyStrokeForEvent arg)
    :else (if-let [ks (KeyStroke/getKeyStroke ^String (preprocess-descriptor (str arg)))]
            ks
            (illegal-argument "Invalid keystroke descriptor: %s" arg))))
  ([first-arg & args]))

(defn analyze-keystroke 
  "Convert a keystroke to a map of descriptors.  Note that :typed keystrokes are designed for
   receiving character input, and are therefore the only ones that reliably generate chars; all
   other keystroke types (:pressed and :released) do not generate a char. 
   Meanwhile, :typed keystrokes do not generate meaningful :code values."   
  [arg]
  (if-not (instance? AWTKeyStroke arg) (illegal-argument "Invalid keystroke: %s" arg)
    (let [event-code (.getKeyEventType arg)
          event (event-type event-code)
          modifier-int (.getModifiers arg)]
    {:keystroke (.toString arg)
     :event event
     :event-code event-code
     :char (.getKeyChar arg) 
     :code (.getKeyCode arg)
     :modifier-int modifier-int
     :modifiers (keymask-to-set modifier-int)})))