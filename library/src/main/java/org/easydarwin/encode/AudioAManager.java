package org.easydarwin.encode;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

/**
 * @Author: tobato
 * @Description: 作用描述
 * @CreateDate: 2020/8/14 21:37
 * @UpdateUser: 更新者
 * @UpdateDate: 2020/8/14 21:37
 */
public class AudioAManager {
    int samplingRate = 8000;
    AudioRecord mAudioRecord = null;   // 底层的音频采集


    public static AudioAManager getInstance (){
       return AudioManagerHelper.audioAManager;
    }


    private static class AudioManagerHelper{
      public  static AudioAManager audioAManager = new AudioAManager();
    }

    public AudioRecord  getAudioRecord(){

        // 计算bufferSizeInBytes：int size = 采样率 x 位宽 x 通道数
        int bufferSize = AudioRecord.getMinBufferSize(samplingRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        /*
         * 1、配置参数，初始化AudioRecord构造函数
         * audioSource：音频采集的输入源，DEFAULT（默认），VOICE_RECOGNITION（用于语音识别，等同于DEFAULT），MIC（由手机麦克风输入），VOICE_COMMUNICATION（用于VoIP应用）等等
         * sampleRateInHz：采样率，注意，目前44.1kHz是唯一可以保证兼容所有Android手机的采样率。
         * channelConfig：通道数的配置，CHANNEL_IN_MONO（单通道），CHANNEL_IN_STEREO（双通道）
         * audioFormat：配置“数据位宽”的,ENCODING_PCM_16BIT（16bit），ENCODING_PCM_8BIT（8bit）
         * bufferSizeInBytes：配置的是 AudioRecord 内部的音频缓冲区的大小，该缓冲区的值不能低于一帧“音频帧”（Frame）的大小
         * */

        if (mAudioRecord == null) {
            mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    samplingRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize);
        }
        return mAudioRecord;
    }

    public void releaseAudio(){
        // 4、停止采集，释放资源。
        if (mAudioRecord != null) {
            mAudioRecord.stop();
            mAudioRecord.release();
            mAudioRecord = null;
        }
    }
}
