;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns seesaw.util
  (:require clojure.string 
            [j18n.core :as j18n])
  (:import [java.net URL URI MalformedURLException URISyntaxException]
           java.lang.reflect.Modifier))

(defn illegal-argument 
  "Throw an illegal argument exception formatted as with (clojure.core/format)"
  [fmt & args]
  (throw (IllegalArgumentException. ^String (apply format fmt args))))

(defn check-args 
  [condition message]
  (if-not condition
    (throw (IllegalArgumentException. ^String message))
    true))

(defn root-cause
  [^Throwable e]
  (if-let [cause (.getCause e)]
    (root-cause cause)
    e))

(defmacro cond-doto
  "Spawn of (cond) and (doto). Works like (doto), but each form has a condition
   which controls whether it is executed. Returns x.

  (doto (new java.util.HashMap) 
    true    (.put \"a\" 1) 
    (< 2 1) (.put \"b\" 2))
  
  Here, only (.put \"a\" 1) is executed.
  "
  [x & forms]
    (let [gx (gensym)]
      `(let [~gx ~x]
         ~@(map (fn [[c a]]
                  (if (seq? a)
                    `(when ~c (~(first a) ~gx ~@(next a)))
                    `(when ~c (~a ~gx))))
                (partition 2 forms))
         ~gx)))

(defn to-seq [v]
  "Stupid helper to turn possibly single values into seqs"
  (cond 
    (nil? v) v
    (seq? v)  v
    (coll? v) (seq v)
    (.isArray (class v)) (seq v)
    :else (seq [v])))

(defn- constantize-keyword [k]
  (.. (name k) (toUpperCase) (replace "-" "_")))

(defn- keywordize-constant [c]
  (keyword (.. c (toLowerCase) (replace "_" "-"))))

(defn get-fields [klass]
  "gets and keywordizes all the public static fields declared in klass itself (not in supertypes)"
  (map #(keywordize-constant (.getName %)) 
       (filter #(and (Modifier/isStatic (.getModifiers %)) (#{klass} (.getDeclaringClass %))) (.getFields klass))))

(defn constant-map
  "Given a class and a list of keywordized constant names returns the 
   values of those fields in a map. The name mapping upper-cases and replaces
   hyphens with underscore, e.g.
 
    :above-baseline --> ABOVE_BASELINE

   Note that the fields must be static and declared *in* the class, not a 
   supertype.
   
   Passed only a class, returns a map of all public static constants in the class
  "
  ([^Class klass & fields]
  (let [[options fields] (if (map? (first fields)) [(first fields) (rest fields)] [{} fields])
        {:keys [suffix] :or {suffix ""}} options]
    (reduce
      (fn [m [k v]] (assoc m k v))
      {}
      (map 
        #(vector %1 (.. klass 
                      (getDeclaredField (str (constantize-keyword %1) suffix)) 
                      (get nil)))
        fields))))
  ([^Class klass]
    (apply constant-map klass (get-fields klass))))
    
  
(defn camelize
  "Convert input string to camelCase from hyphen-case"
  [s]
  (clojure.string/replace s #"-(.)" #(.toUpperCase ^String (%1 1))))

(defn boolean? [b]
  "Return true if b is exactly true or false. Useful for handling optional
   boolean properties where we want to do nothing if the property isn't 
   provided."
  (or (true? b) (false? b)))

(defn atom? [a]
  "Return true if a is an atom"
  (isa? (type a) clojure.lang.Atom))

(defn try-cast [c x]
  "Just like clojure.core/cast, but returns nil on failure rather than throwing ClassCastException"
  (try
    (cast c x)
    (catch ClassCastException e nil)))

(defn ^URL to-url [s]
  "Try to parse (str s) as a URL. Returns new java.net.URL on success, nil 
  otherwise. This is different from clojure.java.io/as-url in that it doesn't
  throw an exception and it uses (str) on the input."
  (if (instance? URL s) s
  (try
    (URL. (str s))
    (catch MalformedURLException e nil))))

(defn ^URI to-uri [s]
  "Try to make a java.net.URI from s"
  (cond
    (instance? URI s) s
    (instance? URL s) (.toURI ^URL s)
    :else (try
            (URI. (str s))
            (catch URISyntaxException e nil))))

(defn to-dimension
  [v]
  (cond
    (instance? java.awt.Dimension v) v
    (and (vector? v) (= 3 (count v)) (= :by (second v)))
      (let [[w by h] v] (java.awt.Dimension. w h))
    :else (illegal-argument "v must be a Dimension or [w :by h] got " v)))

(defn to-insets
  [v]
  (cond
    (instance? java.awt.Insets v) v
    (number? v) (java.awt.Insets. v v v v)
    (vector? v) (let [[top left bottom right] v]
                  (java.awt.Insets. top left (or bottom top) (or right left)))
    :else (illegal-argument "Don't know how to create insets from %s" v)))

(defprotocol Children 
  "A protocol for retrieving the children of a widget as a seq. 
  This takes care of idiosyncracies of frame vs. menus, etc."

  (children [c] "Returns a seq of the children of the given widget"))

(extend-protocol Children
  ; Thankfully container covers JComponent, JFrame, dialogs, applets, etc.
  java.awt.Container    (children [this] (seq (.getComponents this)))
  ; Special case for menus. We want the logical menu items, not whatever
  ; junk is used to build them.
  javax.swing.JMenuBar  (children [this] (seq (.getSubElements this)))
  javax.swing.JMenu     (children [this] (seq (.getSubElements this))))

(defn collect
  "Given a root widget or frame, returns a depth-fist seq of all the widgets
  in the hierarchy. For example to disable everything:
  
    (config (collect (.getContentPane my-frame)) :enabled? false)
  "
  [root]
  (tree-seq 
    (constantly true) 
    children
    root))

(defn resource-key?
  "Returns true if v is a i18n resource key, i.e. a namespaced keyword"
  [v]
  (and (keyword? v) (namespace v)))

(defn resource
  [message]
  (if (resource-key? message)
    (j18n/resource message)
    (str message)))

(defn ^Integer to-mnemonic-keycode
  "Convert a character to integer to a mnemonic keycode. In the case of char
  input, generates the correct keycode even if it's lower case. Input argument 
  can be:

  * i18n resource keyword - only first char is used
  * string - only first char is used
  * char   - lower or upper case
  * int    - key event code
  
  See:
    java.awt.event.KeyEvent for list of keycodes
    http://download.oracle.com/javase/6/docs/api/java/awt/event/KeyEvent.html"
  [v]
  (cond 
    (resource-key? v) (to-mnemonic-keycode (resource v))
    (string? v)       (to-mnemonic-keycode (.charAt v 0))
    (char? v)         (int (Character/toUpperCase ^Character v))
    :else             (int v)))

(defmacro define-predicates [body kvs]
  "Take a function body, and a map of keywords to codes, kvs, and define predicates of one argument testing for the codes using body.  The names of the predicates are derived from the keys, appending '?' to the name of each key.  
   The body should take three arguments: the input to the predicate, a key to test against, and a value to test against.  
   For example: 
   (define-predicates (fn [z k x]
                     (or (= z k)
                         (= z x)))
                   {:a 1 :b 2})
   
   will effectively write
   
   (defn a? [x] (or (= x :a)
                    (= x 1)))
   and 
   (defn b? [x] (or (= x :b)
                    (= x 2)))
                             
   Thanks to Cedric Greevey on the Clojure list for help with this macro."
  `(do ~@(map (fn [[k n]] `(defn ~(symbol (str (name k) "?")) [~'x] (~body ~'x ~k ~n))) (eval kvs))))

(defn bit-and? 
  "Shows whether args bitwise-and (i.e. bit-and > 0)"
  ([x y] (> (bit-and x y) 0))
  ([x y & more] (> (apply bit-and x y more) 0))) 

(defn bits-to-set [int-of-bits map-of-items]
  "Converts an int of bits into a set, given the int and a map of the bitmask to the items (or vice-versa)"
  (let [head (first map-of-items)
        [i k] (if (number? (head 0)) [0 1] [1 0])]
  (reduce (fn [accum item] (if (bit-and? int-of-bits (item i))(conj accum (item k)) accum)) #{} map-of-items)))



