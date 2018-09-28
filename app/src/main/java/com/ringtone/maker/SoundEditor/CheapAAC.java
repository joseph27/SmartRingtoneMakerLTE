/*
 * *
 *  * Created by Youssef Assad on 6/2/18 11:17 AM
 *  * Copyright (c) 2018 . All rights reserved.
 *  * Last modified 6/2/18 11:04 AM
 *
 */

package com.ringtone.maker.SoundEditor;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.HashMap;

/**
 * CheapAAC is a CheapSoundFile implementation for AAC (Advanced Audio
 * Codec) encoded sound files.  It supports files with an MP4 header,
 * including unencrypted files encoded by Apple iTunes, and also
 * files with a more basic ADTS header.
 */
public class CheapAAC extends CheapSoundFile {
    public static Factory getFactory() {
        return new Factory() {
            public CheapSoundFile create() {
                return new CheapAAC();
            }

            public String[] getSupportedExtensions() {
                return new String[]{"aac", "m4a"};
            }
        };
    }

    class Atom {
        public int start;
        public int len;  // including header
        public byte[] data;
    }

    private static final int kDINF = 0x64696e66;
    public static final int kFTYP = 0x66747970;
    private static final int kHDLR = 0x68646c72;
    private static final int kMDAT = 0x6d646174;
    private static final int kMDHD = 0x6d646864;
    private static final int kMDIA = 0x6d646961;
    private static final int kMINF = 0x6d696e66;
    private static final int kMOOV = 0x6d6f6f76;
    public static final int kMP4A = 0x6d703461;
    private static final int kMVHD = 0x6d766864;
    private static final int kSMHD = 0x736d6864;
    private static final int kSTBL = 0x7374626c;
    public static final int kSTCO = 0x7374636f;
    public static final int kSTSC = 0x73747363;
    private static final int kSTSD = 0x73747364;
    private static final int kSTSZ = 0x7374737a;
    private static final int kSTTS = 0x73747473;
    private static final int kTKHD = 0x746b6864;
    private static final int kTRAK = 0x7472616b;

    public static final int[] kRequiredAtoms = {
            kDINF,
            kHDLR,
            kMDHD,
            kMDIA,
            kMINF,
            kMOOV,
            kMVHD,
            kSMHD,
            kSTBL,
            kSTSD,
            kSTSZ,
            kSTTS,
            kTKHD,
            kTRAK,
    };

    private static final int[] kSaveDataAtoms = {
            kDINF,
            kHDLR,
            kMDHD,
            kMVHD,
            kSMHD,
            kTKHD,
            kSTSD,
    };

    // Member variables containing frame info

    private HashMap<Integer, Atom> mAtomMap;

    // Member variables containing sound file info

    private int mSamplesPerFrame;

    // Member variables used only while initially parsing the file
    private int mOffset;
    private int mMinGain;
    private int mMaxGain;
    private int mMdatOffset;
    private int mMdatLength;

    private CheapAAC() {
    }

    public int getNumFrames() {
        return mNumFrames;
    }

    public int getSamplesPerFrame() {
        return 1024;
    }

    public int[] getFrameOffsets() {
        return mFrameOffsets;
    }

    public int[] getFrameLens() {
        return mFrameLens;
    }

    public int[] getFrameGains() {
        return mFrameGains;
    }

    public int getFileSizeBytes() {
        return mFileSize;
    }

    public int getAvgBitrateKbps() {
        return mFileSize / (mNumFrames * mSamplesPerFrame);
    }

    public int getSampleRate() {
        return mSampleRate;
    }

    public int getChannels() {
        return mChannels;
    }

    public String getFiletype() {
        return "AAC";
    }

    public String atomToString(int atomType) {
        String str = "";
        str += (char) ((atomType >> 24) & 0xff);
        str += (char) ((atomType >> 16) & 0xff);
        str += (char) ((atomType >> 8) & 0xff);
        str += (char) (atomType & 0xff);
        return str;
    }



    private void parseMp4(InputStream stream, int maxLen)
            throws IOException {
        /*System.out.println("parseMp4 maxLen = " + maxLen);*/

        while (maxLen > 8) {
            int initialOffset = mOffset;

            byte[] atomHeader = new byte[8];
            stream.read(atomHeader, 0, 8);
            int atomLen =
                    ((0xff & atomHeader[0]) << 24) |
                            ((0xff & atomHeader[1]) << 16) |
                            ((0xff & atomHeader[2]) << 8) |
                            ((0xff & atomHeader[3]));
            /*System.out.println("atomType = " +
                               (char)atomHeader[4] +
                               (char)atomHeader[5] +
                               (char)atomHeader[6] +
                               (char)atomHeader[7] +
                               "  " +
                               "offset = " + mOffset +
                               "  " +
                               "atomLen = " + atomLen);*/
            if (atomLen > maxLen)
                atomLen = maxLen;
            int atomType =
                    ((0xff & atomHeader[4]) << 24) |
                            ((0xff & atomHeader[5]) << 16) |
                            ((0xff & atomHeader[6]) << 8) |
                            ((0xff & atomHeader[7]));

            Atom atom = new Atom();
            atom.start = mOffset;
            atom.len = atomLen;
            mAtomMap.put(atomType, atom);

            mOffset += 8;

            if (atomType == kMOOV ||
                    atomType == kTRAK ||
                    atomType == kMDIA ||
                    atomType == kMINF ||
                    atomType == kSTBL) {
                parseMp4(stream, atomLen);
            } else if (atomType == kSTSZ) {
                parseStsz(stream, atomLen - 8);
            } else if (atomType == kSTTS) {
                parseStts(stream, atomLen - 8);
            } else if (atomType == kMDAT) {
                mMdatOffset = mOffset;
                mMdatLength = atomLen - 8;
            } else {
                for (int savedAtomType : kSaveDataAtoms) {
                    if (savedAtomType == atomType) {
                        byte[] data = new byte[atomLen - 8];
                        stream.read(data, 0, atomLen - 8);
                        mOffset += atomLen - 8;
                        mAtomMap.get(atomType).data = data;
                    }
                }
            }

            if (atomType == kSTSD) {
                parseMp4aFromStsd();
            }

            maxLen -= atomLen;
            int skipLen = atomLen - (mOffset - initialOffset);
            /*System.out.println("* atomLen: " + atomLen);*/
            /*System.out.println("* mOffset: " + mOffset);*/
            /*System.out.println("* initialOffset: " + initialOffset);*/
            /*System.out.println("*   diff: " + (mOffset - initialOffset));*/
            /*System.out.println("* skipLen: " + skipLen);*/

            if (skipLen < 0) {
                throw new IOException(
                        "Went over by " + (-skipLen) + " bytes");
            }

            stream.skip(skipLen);
            mOffset += skipLen;
        }
    }

    private void parseStts(InputStream stream, int maxLen)
            throws IOException {
        byte[] sttsData = new byte[16];
        stream.read(sttsData, 0, 16);
        mOffset += 16;
        mSamplesPerFrame =
                ((0xff & sttsData[12]) << 24) |
                        ((0xff & sttsData[13]) << 16) |
                        ((0xff & sttsData[14]) << 8) |
                        ((0xff & sttsData[15]));

        /*System.out.println("STTS samples per frame: " + mSamplesPerFrame);*/
    }

    private void parseStsz(InputStream stream, int maxLen)
            throws IOException {
        byte[] stszHeader = new byte[12];
        stream.read(stszHeader, 0, 12);
        mOffset += 12;
        mNumFrames =
                ((0xff & stszHeader[8]) << 24) |
                        ((0xff & stszHeader[9]) << 16) |
                        ((0xff & stszHeader[10]) << 8) |
                        ((0xff & stszHeader[11]));
        /*System.out.println("mNumFrames = " + mNumFrames);*/

        mFrameOffsets = new int[mNumFrames];
        mFrameLens = new int[mNumFrames];
        mFrameGains = new int[mNumFrames];
        byte[] frameLenBytes = new byte[4 * mNumFrames];
        stream.read(frameLenBytes, 0, 4 * mNumFrames);
        mOffset += 4 * mNumFrames;
        for (int i = 0; i < mNumFrames; i++) {
            mFrameLens[i] =
                    ((0xff & frameLenBytes[4 * i + 0]) << 24) |
                            ((0xff & frameLenBytes[4 * i + 1]) << 16) |
                            ((0xff & frameLenBytes[4 * i + 2]) << 8) |
                            ((0xff & frameLenBytes[4 * i + 3]));
            /*System.out.println("FrameLen[" + i + "] = " + mFrameLens[i]);*/
        }
    }

    private void parseMp4aFromStsd() {
        byte[] stsdData = mAtomMap.get(kSTSD).data;
        mChannels =
                ((0xff & stsdData[32]) << 8) |
                        ((0xff & stsdData[33]));
        mSampleRate =
                ((0xff & stsdData[40]) << 8) |
                        ((0xff & stsdData[41]));

        /*System.out.println("%% channels = " + mChannels + ", " +
          "sampleRate = " + mSampleRate);*/
    }

    void parseMdat(InputStream stream, int maxLen)
            throws IOException {
        /*System.out.println("***MDAT***");*/
        int initialOffset = mOffset;
        for (int i = 0; i < mNumFrames; i++) {
            mFrameOffsets[i] = mOffset;
            /*System.out.println("&&& start: " + (mOffset - initialOffset));*/
            /*System.out.println("&&& start + len: " +
              (mOffset - initialOffset + mFrameLens[i]));*/
            /*System.out.println("&&& maxLen: " + maxLen);*/

            if (mOffset - initialOffset + mFrameLens[i] > maxLen - 8) {
                mFrameGains[i] = 0;
            } else {
                readFrameAndComputeGain(stream, i);
            }
            if (mFrameGains[i] < mMinGain)
                mMinGain = mFrameGains[i];
            if (mFrameGains[i] > mMaxGain)
                mMaxGain = mFrameGains[i];

            if (mProgressListener != null) {
                boolean keepGoing = mProgressListener.reportProgress(
                        mOffset * 1.0 / mFileSize);
                if (!keepGoing) {
                    break;
                }
            }
        }
    }

    private void readFrameAndComputeGain(InputStream stream, int frameIndex)
            throws IOException {

        if (mFrameLens[frameIndex] < 4) {
            mFrameGains[frameIndex] = 0;
            stream.skip(mFrameLens[frameIndex]);
            return;
        }

        int initialOffset = mOffset;

        byte[] data = new byte[4];
        stream.read(data, 0, 4);
        mOffset += 4;

        /*System.out.println(
            "Block " + frameIndex + ": " +
            data[0] + " " +
            data[1] + " " +
            data[2] + " " +
            data[3]);*/

        int idSynEle = (0xe0 & data[0]) >> 5;
        /*System.out.println("idSynEle = " + idSynEle);*/

        switch (idSynEle) {
            case 0:  // ID_SCE: mono
                int monoGain = ((0x01 & data[0]) << 7) | ((0xfe & data[1]) >> 1);
            /*System.out.println("monoGain = " + monoGain);*/
                mFrameGains[frameIndex] = monoGain;
                break;
            case 1:  // ID_CPE: stereo
                int windowSequence = (0x60 & data[1]) >> 5;
            /*System.out.println("windowSequence = " + windowSequence);*/
                int windowShape = (0x10 & data[1]) >> 4;
            /*System.out.println("windowShape = " + windowShape);*/

                int maxSfb;
                int scaleFactorGrouping;
                int maskPresent;
                int startBit;

                if (windowSequence == 2) {
                    maxSfb = 0x0f & data[1];

                    scaleFactorGrouping = (0xfe & data[2]) >> 1;

                    maskPresent =
                            ((0x01 & data[2]) << 1) |
                                    ((0x80 & data[3]) >> 7);

                    startBit = 25;
                } else {
                    maxSfb =
                            ((0x0f & data[1]) << 2) |
                                    ((0xc0 & data[2]) >> 6);

                    scaleFactorGrouping = -1;

                    maskPresent = (0x18 & data[2]) >> 3;

                    startBit = 21;
                }

            /*System.out.println("maxSfb = " + maxSfb);*/
            /*System.out.println("scaleFactorGrouping = " +
              scaleFactorGrouping);*/
            /*System.out.println("maskPresent = " + maskPresent);*/
            /*System.out.println("startBit = " + startBit);*/

                if (maskPresent == 1) {
                    int sfgZeroBitCount = 0;
                    for (int b = 0; b < 7; b++) {
                        if ((scaleFactorGrouping & (1 << b)) == 0) {
                        /*System.out.println("  1 point for bit " + b +
                                           ": " + (1 << b) +
                                           ", " + (scaleFactorGrouping & (1 << b)));*/
                            sfgZeroBitCount++;
                        }
                    }
                /*System.out.println("sfgZeroBitCount = " + sfgZeroBitCount);*/

                    int numWindowGroups = 1 + sfgZeroBitCount;
                /*System.out.println("numWindowGroups = " + numWindowGroups);*/

                    int skip = maxSfb * numWindowGroups;
                /*System.out.println("skip = " + skip);*/

                    startBit += skip;
                /*System.out.println("new startBit = " + startBit);*/
                }

                // We may need to fill our buffer with more than the 4
                // bytes we've already read, here.
                int bytesNeeded = 1 + ((startBit + 7) / 8);
                byte[] oldData = data;
                data = new byte[bytesNeeded];
                data[0] = oldData[0];
                data[1] = oldData[1];
                data[2] = oldData[2];
                data[3] = oldData[3];
                stream.read(data, 4, bytesNeeded - 4);
                mOffset += (bytesNeeded - 4);
            /*System.out.println("bytesNeeded: " + bytesNeeded);*/

                int firstChannelGain = 0;
                for (int b = 0; b < 8; b++) {
                    int b0 = (b + startBit) / 8;
                    int b1 = 7 - ((b + startBit) % 8);
                    int add = (((1 << b1) & data[b0]) >> b1) << (7 - b);
                /*System.out.println("Bit " + (b  + startBit) + " " +
                                   "b0 " + b0 + " " +
                                   "b1 " + b1 + " " +
                                   "add " + add);*/
                    firstChannelGain += add;
                }
            /*System.out.println("firstChannelGain = " + firstChannelGain);*/

                mFrameGains[frameIndex] = firstChannelGain;
                break;

            default:
                if (frameIndex > 0) {
                    mFrameGains[frameIndex] = mFrameGains[frameIndex - 1];
                } else {
                    mFrameGains[frameIndex] = 0;
                }
            /*System.out.println("Unhandled idSynEle");*/
                break;
        }

        int skip = mFrameLens[frameIndex] - (mOffset - initialOffset);
        /*System.out.println("frameLen = " + mFrameLens[frameIndex]);*/
        /*System.out.println("Skip = " + skip);*/

        stream.skip(skip);
        mOffset += skip;
    }

    private void StartAtom(FileOutputStream out, int atomType)
            throws IOException {
        byte[] atomHeader = new byte[8];
        int atomLen = mAtomMap.get(atomType).len;
        atomHeader[0] = (byte) ((atomLen >> 24) & 0xff);
        atomHeader[1] = (byte) ((atomLen >> 16) & 0xff);
        atomHeader[2] = (byte) ((atomLen >> 8) & 0xff);
        atomHeader[3] = (byte) (atomLen & 0xff);
        atomHeader[4] = (byte) ((atomType >> 24) & 0xff);
        atomHeader[5] = (byte) ((atomType >> 16) & 0xff);
        atomHeader[6] = (byte) ((atomType >> 8) & 0xff);
        atomHeader[7] = (byte) (atomType & 0xff);
        out.write(atomHeader, 0, 8);
    }

    public void WriteAtom(FileOutputStream out, int atomType)
            throws IOException {
        Atom atom = mAtomMap.get(atomType);
        StartAtom(out, atomType);
        out.write(atom.data, 0, atom.len - 8);
    }

    public void SetAtomData(int atomType, byte[] data) {
        Atom atom = mAtomMap.get(atomType);
        if (atom == null) {
            atom = new Atom();
            mAtomMap.put(atomType, atom);
        }
        atom.len = data.length + 8;
        atom.data = data;
    }



    public void WriteFile(File outputFile, int startFrame, int numFrames, boolean fadeIn, boolean fadeOut, int fadeTime)
            throws IOException {
        float startTime = (float)startFrame * getSamplesPerFrame() / mSampleRate;
        float endTime = (float)(startFrame + numFrames) * getSamplesPerFrame() / mSampleRate;
        WriteFile(outputFile, startTime, endTime);
    }

    private void WriteFile(File outputFile, float startTime, float endTime)
            throws IOException {
        int startOffset = (int)(startTime * mSampleRate) * 2 * mChannels;
        int numSamples = (int)((endTime - startTime) * mSampleRate);
        // Some devices have problems reading mono AAC files (e.g. Samsung S3). Making it stereo.
        int numChannels = (mChannels == 1) ? 2 : mChannels;

        String mimeType = "audio/mp4a-latm";
        int bitrate = 64000 * numChannels;  // rule of thumb for a good quality: 64kbps per channel.
        MediaCodec codec = MediaCodec.createEncoderByType(mimeType);
        MediaFormat format = MediaFormat.createAudioFormat(mimeType, mSampleRate, numChannels);
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        codec.start();

        // Get an estimation of the encoded data based on the bitrate. Add 10% to it.
        int estimatedEncodedSize = (int)((endTime - startTime) * (bitrate / 8) * 1.1);
        ByteBuffer encodedBytes = ByteBuffer.allocate(estimatedEncodedSize);
        ByteBuffer[] inputBuffers = codec.getInputBuffers();
        ByteBuffer[] outputBuffers = codec.getOutputBuffers();
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        boolean done_reading = false;
        long presentation_time = 0;

        int frame_size = 1024;  // number of samples per frame per channel for an mp4 (AAC) stream.
        byte buffer[] = new byte[frame_size * numChannels * 2];  // a sample is coded with a short.
        mDecodedBytes.position(startOffset);
        numSamples += (2 * frame_size);  // Adding 2 frames, Cf. priming frames for AAC.
        int tot_num_frames = 1 + (numSamples / frame_size);  // first AAC frame = 2 bytes
        if (numSamples % frame_size != 0) {
            tot_num_frames++;
        }
        int[] frame_sizes = new int[tot_num_frames];
        int num_out_frames = 0;
        int num_frames=0;
        int num_samples_left = numSamples;
        int encodedSamplesSize = 0;  // size of the output buffer containing the encoded samples.
        byte[] encodedSamples = null;
        while (true) {
            // Feed the samples to the encoder.
            int inputBufferIndex = codec.dequeueInputBuffer(100);
            if (!done_reading && inputBufferIndex >= 0) {
                if (num_samples_left <= 0) {
                    // All samples have been read.
                    codec.queueInputBuffer(
                            inputBufferIndex, 0, 0, -1, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    done_reading = true;
                } else {
                    inputBuffers[inputBufferIndex].clear();
                    if (buffer.length > inputBuffers[inputBufferIndex].remaining()) {
                        // Input buffer is smaller than one frame. This should never happen.
                        continue;
                    }
                    // bufferSize is a hack to create a stereo file from a mono stream.
                    int bufferSize = (mChannels == 1) ? (buffer.length / 2) : buffer.length;
                    if (mDecodedBytes.remaining() < bufferSize) {
                        for (int i=mDecodedBytes.remaining(); i < bufferSize; i++) {
                            buffer[i] = 0;  // pad with extra 0s to make a full frame.
                        }
                        mDecodedBytes.get(buffer, 0, mDecodedBytes.remaining());
                    } else {
                        mDecodedBytes.get(buffer, 0, bufferSize);
                    }
                    if (mChannels == 1) {
                        for (int i=bufferSize - 1; i >= 1; i -= 2) {
                            buffer[2*i + 1] = buffer[i];
                            buffer[2*i] = buffer[i-1];
                            buffer[2*i - 1] = buffer[2*i + 1];
                            buffer[2*i - 2] = buffer[2*i];
                        }
                    }
                    num_samples_left -= frame_size;
                    inputBuffers[inputBufferIndex].put(buffer);
                    presentation_time = (long) (((num_frames++) * frame_size * 1e6) / mSampleRate);
                    codec.queueInputBuffer(
                            inputBufferIndex, 0, buffer.length, presentation_time, 0);
                }
            }

            // Get the encoded samples from the encoder.
            int outputBufferIndex = codec.dequeueOutputBuffer(info, 100);
            if (outputBufferIndex >= 0 && info.size > 0 && info.presentationTimeUs >=0) {
                if (num_out_frames < frame_sizes.length) {
                    frame_sizes[num_out_frames++] = info.size;
                }
                if (encodedSamplesSize < info.size) {
                    encodedSamplesSize = info.size;
                    encodedSamples = new byte[encodedSamplesSize];
                }
                outputBuffers[outputBufferIndex].get(encodedSamples, 0, info.size);
                outputBuffers[outputBufferIndex].clear();
                codec.releaseOutputBuffer(outputBufferIndex, false);
                if (encodedBytes.remaining() < info.size) {  // Hopefully this should notification_ic happen.
                    estimatedEncodedSize = (int)(estimatedEncodedSize * 1.2);  // Add 20%.
                    ByteBuffer newEncodedBytes = ByteBuffer.allocate(estimatedEncodedSize);
                    int position = encodedBytes.position();
                    encodedBytes.rewind();
                    newEncodedBytes.put(encodedBytes);
                    encodedBytes = newEncodedBytes;
                    encodedBytes.position(position);
                }
                encodedBytes.put(encodedSamples, 0, info.size);
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                outputBuffers = codec.getOutputBuffers();
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // Subsequent data will conform to new format.
                // We could check that codec.getOutputFormat(), which is the new output format,
                // is what we expect.
            }
            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                // We got all the encoded data from the encoder.
                break;
            }
        }
        int encoded_size = encodedBytes.position();
        encodedBytes.rewind();
        codec.stop();
        codec.release();
        codec = null;

        // Write the encoded stream to the file, 4kB at a time.
        buffer = new byte[4096];
        try {
            FileOutputStream outputStream = new FileOutputStream(outputFile);
            outputStream.write(
                    MP4Header.getMP4Header(mSampleRate, numChannels, frame_sizes, bitrate));
            while (encoded_size - encodedBytes.position() > buffer.length) {
                encodedBytes.get(buffer);
                outputStream.write(buffer);
            }
            int remaining = encoded_size - encodedBytes.position();
            if (remaining > 0) {
                encodedBytes.get(buffer, 0, remaining);
                outputStream.write(buffer, 0, remaining);
            }
            outputStream.close();
        } catch (IOException e) {
           // Log.e("Ringdroid", "Failed to create the .m4a file.");
          //  Log.e("Ringdroid", getStackTrace(e));
        }
    }

    private String mFileType;
    private int mFileSize;
    private int mAvgBitRate;  // Average bit rate in kbps.
    private int mSampleRate;
    private int mChannels;
    private ByteBuffer mDecodedBytes;  // Raw audio data
    private ShortBuffer mDecodedSamples;  // shared buffer with mDecodedBytes.
    private int mNumFrames;
    private int[] mFrameGains;
    private int[] mFrameLens;
    private int[] mFrameOffsets;
    private int mNumSamples;  // total number of samples per channel in audio file



    public void ReadFile(File inputFile)
            throws
            IOException {
        MediaExtractor extractor = new MediaExtractor();
        MediaFormat format = null;
        int i;

        mInputFile = inputFile;
        String[] components = mInputFile.getPath().split("\\.");
        mFileType = components[components.length - 1];
        mFileSize = (int)mInputFile.length();
        extractor.setDataSource(mInputFile.getPath());
        int numTracks = extractor.getTrackCount();
        // find and select the first audio track present in the file.
        for (i=0; i<numTracks; i++) {
            format = extractor.getTrackFormat(i);
            if (format.getString(MediaFormat.KEY_MIME).startsWith("audio/")) {
                extractor.selectTrack(i);
                break;
            }
        }
        if (i == numTracks) {
            try {
                throw new InvalidInputException("No audio track found in " + mInputFile);
            } catch (InvalidInputException e) {
                e.printStackTrace();
            }
        }
        mChannels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        mSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        // Expected total number of samples per channel.
        int expectedNumSamples =
                (int)((format.getLong(MediaFormat.KEY_DURATION) / 1000000.f) * mSampleRate + 0.5f);

        MediaCodec codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME));
        codec.configure(format, null, null, 0);
        codec.start();

        int decodedSamplesSize = 0;  // size of the output buffer containing decoded samples.
        byte[] decodedSamples = null;
        ByteBuffer[] inputBuffers = codec.getInputBuffers();
        ByteBuffer[] outputBuffers = codec.getOutputBuffers();
        int sample_size;
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        long presentation_time;
        int tot_size_read = 0;
        boolean done_reading = false;

        // Set the size of the decoded samples buffer to 1MB (~6sec of a stereo stream at 44.1kHz).
        // For longer streams, the buffer size will be increased later on, calculating a rough
        // estimate of the total size needed to store all the samples in order to resize the buffer
        // only once.
        mDecodedBytes = ByteBuffer.allocate(1<<20);
        Boolean firstSampleData = true;
        while (true) {
            // read data from file and feed it to the decoder input buffers.
            int inputBufferIndex = codec.dequeueInputBuffer(100);
            if (!done_reading && inputBufferIndex >= 0) {
                sample_size = extractor.readSampleData(inputBuffers[inputBufferIndex], 0);
                if (firstSampleData
                        && format.getString(MediaFormat.KEY_MIME).equals("audio/mp4a-latm")
                        && sample_size == 2) {
                    // For some reasons on some devices (e.g. the Samsung S3) you should notification_ic
                    // provide the first two bytes of an AAC stream, otherwise the MediaCodec will
                    // crash. These two bytes do notification_ic contain music data but basic info on the
                    // stream (e.g. channel configuration and sampling frequency), and skipping them
                    // seems OK with other devices (MediaCodec has already been configured and
                    // already knows these parameters).
                    extractor.advance();
                    tot_size_read += sample_size;
                } else if (sample_size < 0) {
                    // All samples have been read.
                    codec.queueInputBuffer(
                            inputBufferIndex, 0, 0, -1, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    done_reading = true;
                } else {
                    presentation_time = extractor.getSampleTime();
                    codec.queueInputBuffer(inputBufferIndex, 0, sample_size, presentation_time, 0);
                    extractor.advance();
                    tot_size_read += sample_size;
                    if (mProgressListener != null) {
                        if (!mProgressListener.reportProgress((float)(tot_size_read) / mFileSize)) {
                            // We are asked to stop reading the file. Returning immediately. The
                            // SoundFile object is invalid and should NOT be used afterward!
                            extractor.release();
                            extractor = null;
                            codec.stop();
                            codec.release();
                            codec = null;
                            return;
                        }
                    }
                }
                firstSampleData = false;
            }

            // Get decoded stream from the decoder output buffers.
            int outputBufferIndex = codec.dequeueOutputBuffer(info, 100);
            if (outputBufferIndex >= 0 && info.size > 0) {
                if (decodedSamplesSize < info.size) {
                    decodedSamplesSize = info.size;
                    decodedSamples = new byte[decodedSamplesSize];
                }
                outputBuffers[outputBufferIndex].get(decodedSamples, 0, info.size);
                outputBuffers[outputBufferIndex].clear();
                // Check if buffer is big enough. Resize it if it's too small.
                if (mDecodedBytes.remaining() < info.size) {
                    // Getting a rough estimate of the total size, allocate 20% more, and
                    // make sure to allocate at least 5MB more than the initial size.
                    int position = mDecodedBytes.position();
                    int newSize = (int)((position * (1.0 * mFileSize / tot_size_read)) * 1.2);
                    if (newSize - position < info.size + 5 * (1<<20)) {
                        newSize = position + info.size + 5 * (1<<20);
                    }
                    ByteBuffer newDecodedBytes = null;
                    // Try to allocate memory. If we are OOM, try to run the garbage collector.
                    int retry = 10;
                    while(retry > 0) {
                        try {
                            newDecodedBytes = ByteBuffer.allocate(newSize);
                            break;
                        } catch (OutOfMemoryError oome) {
                            // setting android:largeHeap="true" in <application> seem to help notification_ic
                            // reaching this section.
                            retry--;
                        }
                    }
                    if (retry == 0) {
                        // Failed to allocate memory... Stop reading more data and finalize the
                        // instance with the data decoded so far.
                        break;
                    }
                    //ByteBuffer newDecodedBytes = ByteBuffer.allocate(newSize);
                    mDecodedBytes.rewind();
                    newDecodedBytes.put(mDecodedBytes);
                    mDecodedBytes = newDecodedBytes;
                    mDecodedBytes.position(position);
                }
                mDecodedBytes.put(decodedSamples, 0, info.size);
                codec.releaseOutputBuffer(outputBufferIndex, false);
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                outputBuffers = codec.getOutputBuffers();
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // Subsequent data will conform to new format.
                // We could check that codec.getOutputFormat(), which is the new output format,
                // is what we expect.
            }
            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0
                    || (mDecodedBytes.position() / (2 * mChannels)) >= expectedNumSamples) {
                // We got all the decoded data from the decoder. Stop here.
                // Theoretically dequeueOutputBuffer(info, ...) should have set info.flags to
                // MediaCodec.BUFFER_FLAG_END_OF_STREAM. However some phones (e.g. Samsung S3)
                // won't do that for some files (e.g. with mono AAC files), in which case subsequent
                // calls to dequeueOutputBuffer may result in the application crashing, without
                // even an exception being thrown... Hence the second check.
                // (for mono AAC files, the S3 will actually double each sample, as if the stream
                // was stereo. The resulting stream is half what it's supposed to be and with a much
                // lower pitch.)
                break;
            }
        }
        mNumSamples = mDecodedBytes.position() / (mChannels * 2);  // One sample = 2 bytes.
        mDecodedBytes.rewind();
        mDecodedBytes.order(ByteOrder.LITTLE_ENDIAN);
        mDecodedSamples = mDecodedBytes.asShortBuffer();
        mAvgBitRate = (int)((mFileSize * 8) * ((float)mSampleRate / mNumSamples) / 1000);

        extractor.release();
        extractor = null;
        codec.stop();
        codec.release();
        codec = null;

        // Temporary hack to make it work with the old version.
        mNumFrames = mNumSamples / getSamplesPerFrame();
        if (mNumSamples % getSamplesPerFrame() != 0){
            mNumFrames++;
        }
        mFrameGains = new int[mNumFrames];
        mFrameLens = new int[mNumFrames];
        mFrameOffsets = new int[mNumFrames];
        int j;
        int gain, value;
        int frameLens = (int)((1000 * mAvgBitRate / 8) *
                ((float)getSamplesPerFrame() / mSampleRate));
        for (i=0; i<mNumFrames; i++){
            gain = -1;
            for(j=0; j<getSamplesPerFrame(); j++) {
                value = 0;
                for (int k=0; k<mChannels; k++) {
                    if (mDecodedSamples.remaining() > 0) {
                        value += Math.abs(mDecodedSamples.get());
                    }
                }
                value /= mChannels;
                if (gain < value) {
                    gain = value;
                }
            }
            mFrameGains[i] = (int) Math.sqrt(gain);  // here gain = sqrt(max value of 1st channel)...
            mFrameLens[i] = frameLens;  // totally notification_ic accurate...
            mFrameOffsets[i] = (int)(i * (1000 * mAvgBitRate / 8) *  //  = i * frameLens
                    ((float)getSamplesPerFrame() / mSampleRate));
        }
        mDecodedSamples.rewind();
        // DumpSamples();  // Uncomment this line to dump the samples in a TSV file.
    }



    public class InvalidInputException extends Exception {
        // Serial version ID generated by Eclipse.
        private static final long serialVersionUID = -2505698991597837165L;
        public InvalidInputException(String message) {
            super(message);
        }
    }


}