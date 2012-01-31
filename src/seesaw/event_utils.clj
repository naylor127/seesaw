;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns 
  ^{:doc "Utilities for working with events.  See http://docs.oracle.com/javase/6/docs/api/java/awt/event/InputEvent.html"}
  seesaw.event-utils
  (:use [seesaw.util :only [illegal-argument define-predicates bits-to-set constant-map]]
        [clojure.set :only [map-invert]])
  (:import [java.util EventObject]
           [java.awt.event KeyEvent ActionEvent ComponentEvent FocusEvent MouseEvent InputEvent]))


(def ^{:private true} type-map (conj 
                (constant-map KeyEvent :key-pressed :key-released :key-typed)
                (constant-map ActionEvent :action-performed)
                (constant-map ComponentEvent :component-hidden :component-moved :component-resized :component-shown)
                (constant-map FocusEvent :focus-gained :focus-lost)
                (constant-map MouseEvent :mouse-clicked :mouse-entered :mouse-exited :mouse-pressed :mouse-released
                              :mouse-dragged :mouse-moved)
                ))

(def ^{:private true} mask-map
  (constant-map InputEvent)) 

(defn event-type 
  "tell me what kind of event this is: returns a keyword"
  [e]
  (cond 
    (instance? EventObject e) (type-map (.getID e))
    (integer? e) (type-map e)
    :else (illegal-argument "Invalid event: %s" e)))

(defn consume!
  "Consumes this event so that it will not be processed in the default manner by the source 
   which originated it.  "
  [e]
  (.consume e))

(defn consumed?
  "Returns whether or not this event has been consumed."
  [e]
  (.isConsumed e))

(defn event-time
  "Returns the timestamp of when this event occurred."
  [e]
  (.getWhen e))

(defn keymask-to-set [modifier-int]
  (bits-to-set modifier-int mask-map))

(defn analyze-keyevent 
  "Convert a key-event to a map of descriptors."   
  [arg]
  (if-not (instance? KeyEvent arg) (illegal-argument "Invalid key-event: %s" arg)
    (let [event-code (.getID arg)
          event (event-type event-code)
          modifier-int (.getModifiers arg)]
      {:char (.getKeyChar arg) 
       :code (.getKeyCode arg)
       :location (.getKeyLocation arg)
       :modifier-int modifier-int
       :modifiers (keymask-to-set modifier-int)
       :modifier-text (KeyEvent/getKeyModifiersText modifier-int)
       :key-text (KeyEvent/getKeyText (.getKeyCode arg))
       :action? (.isActionKey arg)
       :event-string (.toString arg)
       :event-parameters (.paramString arg)
       :event event
       :event-code event-code     
       })))

;generate predicates to check if the event is of a given type
(define-predicates (fn [e k n] 
                   (cond
                     (instance? EventObject e) (= (.getID e) n)
                     (integer? e) (= e n)
                     (keyword? e) (= e k)
                     :else (illegal-argument "Invalid event: %s" e)))
                 type-map)


