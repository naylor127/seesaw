(ns ^{:doc "Window to learn about keystrokes and keycodes.  Run (start-explorer) to use."}   
  seesaw.keystroke-explorer
  (:use seesaw.core seesaw.keymap seesaw.mig seesaw.dev seesaw.keystroke
        [clojure.stacktrace :only [e]] [clojure.repl :only [doc]])  
  (:import javax.swing.KeyStroke java.awt.event.KeyEvent))

(def cf (frame :title "to enter keycodes" :minimum-size [1700 :by 150]))

(def code-entry (text :minimum-size [70 :by 18]))

(def code-display (text :minimum-size [70 :by 18]))

(def keystroke-display (text :minimum-size [250 :by 18]))

(def keystroke-analyzed (text :minimum-size [180 :by 40] :multi-line? true))

(def cp (mig-panel :constraints ["wrap 2"]
                   :items [["enter key here"]
                           [code-entry]
                           ["keycode"]
                           [code-display]
                           ["keystroke"]
                           [keystroke-display]
                           ["analyzed keystroke"]
                           [keystroke-analyzed "span" "growy" "growx"]    
                           ]))

(defn showf [f] (-> f show! pack!))

(defn display [f content]
  (config! f :content content)
  content)

(def code-listen (listen code-entry :key-pressed 
               (fn [e] (do 
                         (config! code-display :text
                                  (.. (KeyStroke/getKeyStrokeForEvent e) getKeyCode))
                         (config! keystroke-display :text
                                  (.. (KeyStroke/getKeyStrokeForEvent e) toString))
                         (config! keystroke-analyzed :text
                                  (str (analyze-keystroke (keystroke e))))))))
  

(defn start-explorer [] (do
                   (display cf cp)
                   (showf cf)))

