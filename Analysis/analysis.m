%read a noise file and perform some analysis on it to see if its working
clear all;
clc;
%reading any random audio file
audioFileName = ['room acoustics',' ', num2str(randi(15)), '.wav'];
[in,samplingFreq]=audioread(audioFileName);
%normalizing input
in = in./abs(max(in));
sampleTime = length(in)/samplingFreq;
numPts = sampleTime * samplingFreq;
timeDataNormalDisplay = in; % 0 is at centre
timeDataFolded = fftshift(timeDataNormalDisplay); % 0 is at 0th position

%generating fft values
freqDataFolded = fftshift(abs(fft(timeDataFolded)));
freqValues = (samplingFreq * (-numPts/2:numPts/2-1))/numPts;

figure(1);subplot(2,1,1);
plot(freqValues, freqDataFolded); title('Frequency domain plot');
xlabel('Frequency in Hz'); ylabel ('Magnitude');

%Generating average over 1/8Hz
averageOver = 1/8;
freqDeltaF = samplingFreq/numPts;
ptsAverageOverFreq = floor (averageOver/freqDeltaF);
numPtsAfterAverage = floor(numPts/ptsAverageOverFreq); 
arrayOfFFTAverages = zeros(numPtsAfterAverage,1);
arrayOfFreqAverages = zeros(numPtsAfterAverage,1);
count = 1;
for n = 1 : ptsAverageOverFreq : numPts - ptsAverageOverFreq
    arrayOfFFTAverages (count) = mean (freqDataFolded(n:n+ptsAverageOverFreq-1));
    arrayOfFreqAverages (count) = mean (freqValues(n:n+ptsAverageOverFreq-1));
    count = count + 1;
end

%keep only positive frequency values upto 300Hz
arrayOfFFTAverages = arrayOfFFTAverages(numPtsAfterAverage/2+1:end);
arrayOfFreqAverages = arrayOfFreqAverages(numPtsAfterAverage/2+1:end);
upperLimitFreq = 300; 
freqDeltaFAfterAverage = samplingFreq/numPtsAfterAverage;
ptsTillUpperLimit = floor(upperLimitFreq/freqDeltaFAfterAverage);
arrayOfFFTAverages = arrayOfFFTAverages(1 : ptsTillUpperLimit);
arrayOfFreqAverages = arrayOfFreqAverages(1 : ptsTillUpperLimit);
strongestSignal = max(arrayOfFFTAverages); 
strongestSignal_freq = max(arrayOfFreqAverages(find(arrayOfFFTAverages == strongestSignal)));

figure(1);subplot(2,1,2);
plot(arrayOfFreqAverages, arrayOfFFTAverages,'r');title('Averaged frequency domain plot');
xlabel('Frequency in Hz');ylabel('Magnitude');


%breaking into frequency bands
numPtsInEachBand = floor(15/freqDeltaFAfterAverage);
freqBandYvals = zeros(32,numPtsInEachBand);
freqBandXvals = zeros(32,numPtsInEachBand);

for n =0:31
    startFreq= 2 + 5*n;
    startingPt = floor(startFreq/freqDeltaFAfterAverage);
    freqBandYvals (n+1,:) = 20*log10(arrayOfFFTAverages(startingPt : startingPt + numPtsInEachBand-1));
    freqBandXvals (n+1,:) = arrayOfFreqAverages(startingPt : startingPt + numPtsInEachBand-1);   
end


%identify strongest signal in each band and average band power
strongestSignalInBand = zeros(32,1);
strongestSignalInBand_freq = zeros(32,1);
avgBandPower = zeros(32,1);
for n = 1:32
    strongestSignalInBand(n) = max(freqBandYvals(n,:));
    strongestSignalInBand_freq(n) = freqBandXvals(n,find(freqBandYvals(n,:) == strongestSignalInBand(n)));
    avgBandPower(n) = mean(freqBandYvals(n,:));
end


%calculating PERCENTAGE_WORSE_CASE
%strongestSignal - freqBandYvals(n,k) < 0.5 * avg deviation of
%freqBandYvals from strongestSignal
meu_strong = mean(sqrt(mean((20*log10(strongestSignal) - freqBandYvals).^2)));
%threshold = 0.8;
threshold = 1 - (0.5 * meu_strong)/(20*log10(strongestSignal));


Percentage_worse_case = zeros(32,numPtsInEachBand);
 for n =1:32
     for k=1:numPtsInEachBand
        
         if((freqBandYvals(n,k)/(20*log10(strongestSignal))) >= threshold) 
             Percentage_worse_case(n,k) = freqBandXvals(n,k);
         end
     end
 end


%calculating ratio of power across 1/8Hz bandwidth of signal to average band
%power and finding out the weakest signals
number_weak_peaks = zeros(32,1);

for n =1:32
    for k =1:numPtsInEachBand
        standard_dev = std(freqBandYvals(n,:));
        weak_threshold = 1 -(1.5 * standard_dev)/avgBandPower(n);
        %avgBandPower(n) - freqBandYvals(n,k) >1.5 *standard deviation of freqBandYvals(n,:)
        if(freqBandYvals(n,k)/avgBandPower(n)<=weak_threshold) 
            number_weak_peaks(n) = number_weak_peaks(n) + 1;
        end
    end   
end
%finding out which frequency band has lowest number of peaks
%a number between 1 and 32  
lowestPeakBand = find(number_weak_peaks == max(number_weak_peaks),1);


%calculating RATIO_BACKGROUND_NOISE - find out the frequency band with the
%greatest number of weak signals, and find the ratio of each signal to the
%avg power of the weakest band

avgPower_lowestPeakBand = mean(freqBandYvals (lowestPeakBand,:));
meu_weak = mean(sqrt(mean((avgPower_lowestPeakBand - freqBandYvals).^2)));
%freqBandvals(n,k) - avgPower_lowestPeakBand >  avg deviation of
%freqBandYvals from avgPower_lowestPeakBand
limit = 1 + (meu_weak)/avgPower_lowestPeakBand;
Ratio_background_noise = zeros(32,numPtsInEachBand);

for n=1:32
    for k=1:numPtsInEachBand
         %peaks which are greater than 120% of average power of weakest frequency band
        if (freqBandYvals(n,k)/avgPower_lowestPeakBand >= limit)
            Ratio_background_noise(n,k) = freqBandXvals(n,k);
        end        
    end
end



%connecting to database
conn = database.ODBCConnection('dsp_results','root','analyse_hum');
if conn == false
    disp('connection unsuccessful');
else
    disp ('connection successful');
end

sqlquery = 'CREATE TABLE IF NOT EXISTS analysis_data (Name varchar(50), Time varchar(50), Maximum_Signal_Frequency double, Performance_worse_case_Hz double, Ratio_background_noise_Hz double)';
if(exec(conn,sqlquery) == false)
    disp('table not created');
else
    disp('table created successfully');
end

%inserting data into database
index_percentage = find (Percentage_worse_case);
Percentage_worse_case_db = zeros(length(index_percentage),1);
for n=1:length(index_percentage)
    Percentage_worse_case_db(n) = Percentage_worse_case(index_percentage(n));
end
Percentage_worse_case_db = sort(unique(Percentage_worse_case_db));

index_ratio = find (Ratio_background_noise);
Ratio_background_noise_db = zeros(length(index_ratio),1);
for n=1:length(index_ratio)
        Ratio_background_noise_db(n) = Ratio_background_noise(index_ratio(n));
end
Ratio_background_noise_db = sort(unique(Ratio_background_noise_db));

%plotting current histogram of current data
figure(2);
pbins = min(Percentage_worse_case_db) : 5 :max(Percentage_worse_case_db);
subplot(2,1,1);hist(Percentage_worse_case_db, pbins); xlabel('Frequency bins in Hz');ylabel('Number of occurrences');
title('Percentage worse case');
rbins = min(Ratio_background_noise_db):5:max(Ratio_background_noise_db);
subplot(2,1,2);hist(Ratio_background_noise_db,rbins);

%Ratio_background_noise also contains Percentage_worse_case frequencies.
%They should be eliminated.
ismem = ismember(Ratio_background_noise_db, Percentage_worse_case_db);
Ratio_background_noise_db_final = Ratio_background_noise_db;xlabel('Frequency bins in Hz');ylabel('Number of occurrences');
title('Ratio background noise');


colnames = {'Name','Time','Maximum_signal_frequency','Performance_worse_case_Hz','Ratio_background_noise_Hz'};
minimum = min(length(Ratio_background_noise_db_final), length(Percentage_worse_case_db));
maximum = max(length(Ratio_background_noise_db_final), length(Percentage_worse_case_db));
for n = 1:minimum
    data = {audioFileName, datestr(clock), strongestSignal_freq, Percentage_worse_case_db(n),Ratio_background_noise_db_final(n)};
    fastinsert(conn,'analysis_data',colnames,data);
end

for n = minimum + 1 : maximum    
    if(length(Ratio_background_noise_db_final) > length(Percentage_worse_case_db))
       data = {audioFileName, datestr(clock), strongestSignal_freq, 0.0, Ratio_background_noise_db_final(n)};
    else
       data = {audioFileName, datestr(clock), strongestSignal_freq, Percentage_worse_case_db(n), 0.0}; 
    end
    fastinsert(conn,'analysis_data',colnames,data);
end
        
    
%displaying data
% curs = exec(conn,'select * from analysis_data');
% curs = fetch (curs);
% curs.Data
% close(curs);

%plotting histogram according to date
curs = exec(conn,'select distinct Time from analysis_data');
curs = fetch(curs);
dateList = curs.Data
pos = input('Enter date position: ');
%cellstr object needs to be converted to string
date = strcat('''', dateList{pos}, '''')
select = 'SELECT Performance_worse_case_Hz,Ratio_background_noise_Hz FROM analysis_data WHERE Time = ';
query = strcat(select, date)
curs = exec(conn, query);
curs = fetch(curs);
curs.Data(1,:)
close(curs);


%delete table if user says so
if 1
str = input('Delete table?[y/n]:' ,'s');
if(str == 'y')
    sqlquery = 'DROP TABLE analysis_data';
    if(exec(conn,sqlquery) == true)
        disp('Table deleted successfully');
    end
end
end
close(conn);
