(ns org.bandm.android.SoundActivity
  (:gen-class :extends android.app.Activity
              :main false
              :exposes-methods {onCreate superOnCreate})
  (:import [android.view KeyEvent View$OnClickListener]
           [android.webkit WebSettings$ZoomDensity WebViewClient]
           [org.bandm.android R$id R$layout]
           ))

(defn startRecording [startRecBtn stopRecBtn]
  (.setEnabled startRecBtn false)
  (.setEnabled stopRecBtn true)
  (.requestFocus stopRecBtn)
  )

(defn stopRecording [startRecBtn stopRecBtn]
  (.setEnabled startRecBtn true)
  (.setEnabled stopRecBtn false)
  (.requestFocus startRecBtn)
  )

(defn clearHandler [textField]
  (let [
        randNum (java.lang.Math/ceil(* (java.lang.Math/random) 100))
        ]
      (.setText textField (apply str ["clj-random: " randNum]))
      ;(.setText textField "clj-random:")
      ))

(defn -onCreate
  "Called when the activity is initialised."
  [this bundle]
  (doto this
    (.superOnCreate bundle)
    (.setContentView R$layout/main)
    )
  (let 
    [
     valRevolvingSpeed (.findViewById this R$id/valRevolvingSpeed)
     startRecBtn (.findViewById this R$id/start)
     stopRecBtn (.findViewById this R$id/stop)
     clearBtn (.findViewById this R$id/clear)
     ]
    
    (.setEnabled stopRecBtn false)
    (.setEnabled startRecBtn true)
    
    (.setOnClickListener clearBtn
      (proxy [View$OnClickListener] []
        (onClick [view]
                 (clearHandler valRevolvingSpeed)
                 )))

    (.setOnClickListener startRecBtn
      (proxy [View$OnClickListener] []
        (onClick [view]
                 (startRecording startRecBtn stopRecBtn)
                 )))

    (.setOnClickListener stopRecBtn
      (proxy [View$OnClickListener] []
        (onClick [view]
                 (stopRecording startRecBtn stopRecBtn)
                 )))

    ))
