# Hum-App
Android application to capture, store and record low frequency noise - developed during Mitacs internship at University of Calgary, 2015.

The Ranchlands' Hum is a low frequency urban noise nuisance that has been plaguing the residents of Calgary, Canada for years. Its source is yet undetected. This 
application was developed during a 3 month internship in University of Calgary to capture, store and analyze low frequency audio resembling the Ranchlands' Hum. Its 
features include:

1) Ability to capture 10 seconds of noise in bursts, and plot its frequency spectrum by using the FFT algorithm . 2) Calculate performance metrics to determine 
strongest frequencies (high SNR - Percentage Worse Case Frequencies) and frequencies overshadowed by background noise (low SNR - Ratio Background Noise Frequencies) 
and plot their histogram. 3) Store all the relevant information in an SQLite database and integrate it with the UI so that any recording from any time can be accessed 
and compared with other recordings, if desired.The graphs and histograms are plotted with the GraphView library.
