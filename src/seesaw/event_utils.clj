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
  (:use [seesaw.util :only [illegal-argument make-predicates bits-to-set]]
        [clojure.set :only [map-invert]])
  (:import [java.util EventObject]))

(def ^{:private true} type-map {401 :key-pressed 402 :key-released 400 :key-typed 1001 :action-performed 103 :component-hidden
                                100 :component-moved 101 :component-resized 102 :component-shown 1004 :focus-gained 1005 :focus-lost
                                500 :mouse-clicked 504 :mouse-entered 505 :mouse-exited 501 :mouse-pressed 502 :mouse-released
                                506 :mouse-dragged 503 :mouse-moved})

(def keywords-to-masks {:alt-down 512 :alt-graph-down 8192 :alt-graph 32 :alt 8 :button1-down 1024 :button1 16
     :button2-down 2048 :button2 8 :button3-down 4096 :button3 4 :ctrl-down 128 :ctrl 2
     :meta-down 256 :meta 4 :shift-down 64 :shift 5})

(def masks-to-keywords (map-invert keywords-to-masks))

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
  (bits-to-set modifier-int keywords-to-masks))

;generate predicates to check if the event is of a given type
(make-predicates (fn [e k n] 
                   (cond
                     (instance? EventObject e) (= (.getID e) n)
                     (integer? e) (= e n)
                     (keyword? e) (= e k)
                     :else (illegal-argument "Invalid event: %s" e)))
                 type-map)


