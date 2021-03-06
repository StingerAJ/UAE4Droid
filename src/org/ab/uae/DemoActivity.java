/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This is a small port of the "San Angeles Observation" demo
 * program for OpenGL ES 1.x. For more details, see:
 *
 *    http://jet.ro/visuals/san-angeles-observation/
 *
 * This program demonstrates how to use a GLSurfaceView from Java
 * along with native OpenGL calls to perform frame rendering.
 *
 * Touching the screen will start/stop the animation.
 *
 * Note that the demo runs much faster on the emulator than on
 * real devices, this is mainly due to the following facts:
 *
 * - the demo sends bazillions of polygons to OpenGL without
 *   even trying to do culling. Most of them are clearly out
 *   of view.
 *
 * - on a real device, the GPU bus is the real bottleneck
 *   that prevent the demo from getting acceptable performance.
 *
 * - the software OpenGL engine used in the emulator uses
 *   the system bus instead, and its code rocks :-)
 *
 * Fixing the program to send less polygons to the GPU is left
 * as an exercise to the reader. As always, patches welcomed :-)
 */
package org.ab.uae;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.ab.controls.GameKeyListener;
import org.ab.controls.VirtualKeypad;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.inputmethodservice.KeyboardView.OnKeyboardActionListener;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;


// TODO: export vibrator to SDL - interface is available in SDL 1.3

class Globals {
	public static String ApplicationName = "uae";
	// Should be zip file
	public static String DataDownloadUrl = "http://sites.google.com/site/xpelyax/Home/alienblaster110_data.zip?attredirects=0&d=1";
	// Set DownloadToSdcard to true if your app data is bigger than 5 megabytes.
	// It will download app data to /sdcard/alienblaster then,
	// otherwise it will download it to /data/data/de.schwardtnet.alienblaster/files -
	// set this dir in jni/Android.mk in SDL_CURDIR_PATH
	public static boolean DownloadToSdcard = false;
	
	public static String PREFKEY_ROM = "rom_location";
	public static int PREFKEY_ROM_INT = 0;
	
	public static String PREFKEY_F1 = "f1_location";
	public static int PREFKEY_F1_INT = 1;
	
	public static String PREFKEY_F2 = "f2_location";
	public static int PREFKEY_F2_INT = 2;
	
	public static String PREFKEY_F3 = "f3_location";
	public static int PREFKEY_F3_INT = 3;
	
	public static String PREFKEY_F4 = "f4_location";
	public static int PREFKEY_F4_INT = 4;
	
	public static String PREFKEY_SOUND = "sound";
	public static String PREFKEY_DRIVESTATUS = "drivestatus";
	public static String PREFKEY_NTSC = "ntsc";
	public static String PREFKEY_AFS = "auto_frameskip";
	public static String PREFKEY_FS = "frameskip";
	public static String PREFKEY_SC = "system_clock";
	public static String PREFKEY_ST = "sync_threshold";
	
	public static String PREFKEY_CYCLONE = "cyclone_core";
	
	public static String PREFKEY_START = "start";
}

/*
class AudioThread extends Thread {

	private Activity mParent;
	private AudioTrack mAudio;
	private byte[] mAudioBuffer;
	private ByteBuffer mAudioBufferNative;

	public AudioThread(Activity parent)
	{
		mParent = parent;
		mAudio = null;
		mAudioBuffer = null;
		this.setPriority(Thread.MAX_PRIORITY);
		this.start();
	}
	
	@Override
	public void run() 
	{
		while( !isInterrupted() )
		{
			if( mAudio == null )
			{
				int[] initParams = nativeAudioInit();
				if( initParams == null )
				{
					try {
						sleep(200);
					} catch( java.lang.InterruptedException e ) { };
				}
				else
				{
					int rate = initParams[0];
					int channels = initParams[1];
					channels = ( channels == 1 ) ? AudioFormat.CHANNEL_CONFIGURATION_MONO : 
													AudioFormat.CHANNEL_CONFIGURATION_STEREO;
					int encoding = initParams[2];
					encoding = ( encoding == 1 ) ? AudioFormat.ENCODING_PCM_16BIT :
													AudioFormat.ENCODING_PCM_8BIT;
					int bufSize = AudioTrack.getMinBufferSize( rate, channels, encoding );
					bufSize = bufSize;
					if( initParams[3] > bufSize )
						bufSize = initParams[3];
					mAudioBuffer = new byte[bufSize];
					nativeAudioInit2(mAudioBuffer);
					mAudio = new AudioTrack(AudioManager.STREAM_MUSIC, 
												rate,
												channels,
												encoding,
												bufSize,
												AudioTrack.MODE_STREAM );
					mAudio.play();
				}
			}
			else
			{
				int len = nativeAudioBufferLock();
				if( len > 0 )
					mAudio.write( mAudioBuffer, 0, len );
				if( len < 0 )
					break;
				nativeAudioBufferUnlock();
			}
		}
		if( mAudio != null )
		{
			mAudio.stop();
			mAudio.release();
			mAudio = null;
		}
	}
	
	private static native int[] nativeAudioInit();
	private static native int nativeAudioInit2(byte[] buf);
	private static native int nativeAudioBufferLock();
	private static native int nativeAudioBufferUnlock();
}*/
public class DemoActivity extends Activity implements GameKeyListener {
private int currentprimaryCode; // no multitouch

protected VirtualKeypad vKeyPad = null;
	
	public class theKeyboardActionListener implements OnKeyboardActionListener{

        public void onKey(int primaryCode, int[] keyCodes) {
        	manageOnKey(primaryCode);
        }

        public void onPress(int primaryCode) {
        	if (mGLView != null) {
        		if (currentprimaryCode > -1)
        			mGLView.actionKey(false, currentprimaryCode);
        		mGLView.actionKey(true, primaryCode);
        		currentprimaryCode = primaryCode;
        	}
        }

        public void onRelease(int primaryCode) {
        	if (mGLView != null) {
        		if (currentprimaryCode > -1 && currentprimaryCode == primaryCode)
        			mGLView.actionKey(false, primaryCode);
        		else {
        			if (currentprimaryCode > -1)
        				mGLView.actionKey(false, currentprimaryCode);
        			else
        				mGLView.actionKey(false, primaryCode);
        			currentprimaryCode = -1;
        		}
        	}
        		
        }

        public void onText(CharSequence text) {}

     
        public void swipeDown() {}

     
        public void swipeLeft() {}

    
        public void swipeRight() {}

     
        public void swipeUp() {}};
        
        protected void manageOnKey(int c) {
    		if (c == Keyboard.KEYCODE_MODE_CHANGE) {
    			// switch layout
    			if (currentKeyboardLayout == 1)
    				switchKeyboard(2, true);
    			else if (currentKeyboardLayout == 2)
    				switchKeyboard(1, true);
    		} 
    	}
        
        protected void switchKeyboard(int newLayout, boolean preview) {
    		currentKeyboardLayout = newLayout;
    		if (theKeyboard != null) {
    			SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
    			boolean oldJoystick = sp.getBoolean("oldJoystick", false);
    			if (oldJoystick) {
    				theKeyboard.setKeyboard(layouts[currentKeyboardLayout]);
    				theKeyboard.setPreviewEnabled(preview);
    				if (mGLView != null) {
    					mGLView.shiftImage(touch&&joystick==1?SHIFT_KEYB:0);
    				}
    				vKeyPad = null;
    			} else {
	    			if (currentKeyboardLayout == 0) {
	    				 // on vire le keypad Android pour mettre celui de yongzh
						theKeyboard.setVisibility(View.INVISIBLE);
						vKeyPad = new VirtualKeypad(mGLView, this, R.drawable.dpad, R.drawable.button);
						if (mGLView.getWidth() > 0)
							vKeyPad.resize(mGLView.getWidth(), mGLView.getHeight());
	    			} else {
	    				theKeyboard.setKeyboard(layouts[currentKeyboardLayout]);
	    				theKeyboard.setPreviewEnabled(preview);
	    				theKeyboard.setVisibility(touch?View.VISIBLE:View.INVISIBLE);
	    				vKeyPad = null;
	    			}
    			}
    		}
    	}
        
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        requestWindowFeature(Window.FEATURE_PROGRESS);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN); 
        /*TextView tv = new TextView(this);
        tv.setText("Initializing");
        setContentView(tv);
        downloader = new DataDownloader(this, tv);*/
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        cyclone = sp.getBoolean(Globals.PREFKEY_CYCLONE, false);
    	System.loadLibrary(cyclone?"uaecyclone":"uae");
        checkConf();
        checkFiles(false);
        
        
    }
    
    private boolean cyclone;
    private String romPath = null;
    private String f1Path = null;
    private String f2Path = null;
    private String f3Path = null;
    private String f4Path = null;
    private int sound = 0;
    public int joystick = 1;
    public boolean touch;
    public int mouse_button;
    //private int must_reset = 0;
    
    private void checkConf() {
    	SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
    	current_keycodes = new int [default_keycodes.length];
    	for(int i=0;i<default_keycodes.length;i++)
    		current_keycodes[i] = sp.getInt("key." + default_keycodes_string[i], default_keycodes[i]);
    }
    
    private void checkFiles(boolean force_reset) {
    	
    	File saveDir = new File("/sdcard/.uae");
    	saveDir.mkdir();
    	
    	SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
    	String rPath = sp.getString(Globals.PREFKEY_ROM, null);
    	String f1P = sp.getString(Globals.PREFKEY_F1, null);
    	String f2P = sp.getString(Globals.PREFKEY_F2, null);
    	String f3P = sp.getString(Globals.PREFKEY_F3, null);
    	String f4P = sp.getString(Globals.PREFKEY_F4, null);
    	boolean autofs = sp.getBoolean(Globals.PREFKEY_AFS, true);
    	boolean bsound = sp.getBoolean(Globals.PREFKEY_SOUND, false);
    	boolean drivestatus = sp.getBoolean(Globals.PREFKEY_DRIVESTATUS, false);
    	boolean ntsc = sp.getBoolean(Globals.PREFKEY_NTSC, false);
    	int fs = Integer.parseInt(sp.getString(Globals.PREFKEY_FS, "2"));
    	int sc = Integer.parseInt(sp.getString(Globals.PREFKEY_SC, "0"));
    	int st = Integer.parseInt(sp.getString(Globals.PREFKEY_ST, "0"));
    	boolean first_start = false;
    	boolean changed_disks = false;
    	boolean changed_sound = false;
    	if ((sound == 2 && !bsound) || (sound == 0 && bsound))
    		changed_sound = true;
    	sound = bsound?2:0;
    	if (romPath == null)
    		first_start = true;
    	boolean romChange = false;
    	if (rPath != null && !rPath.equals(romPath)) {
    		if (romPath !=null)
    			romChange = true;
    		romPath = rPath;
    	}
    	if (!first_start && ((f1P != null && !f1P.equals(f1Path)) || (f2P != null && !f2P.equals(f2Path)) || (f3P != null && !f3P.equals(f3Path)) || (f4P != null && !f4P.equals(f4Path)))) {
    		changed_disks = true;
    	}
    	f1Path = f1P;
    	f2Path = f2P;
    	f3Path = f3P;
    	f4Path = f4P;
    	TextView tv = new TextView(this);
        tv.setText("Status:\n");
        boolean romOk = false;
        if (romPath == null)
        	tv.append("ROM not configured\n");
        else {
        	File r = new File(romPath);
        	if (r.exists()&& r.length() > 200000) // lazy check :\
        		romOk = true;
        	else
        		tv.append("ROM invalid\n");
        }
        
        if (romOk) {
        	if (romChange) {
        		 showDialog(2); 
        	} else {
	        	// launch
	        	setPrefs(romPath, f1P, f2P, f3P, f4P, autofs?100:fs, sc, st, changed_sound?1:0, sound, changed_disks?1:0, force_reset&&!first_start?1:0, drivestatus?1:0, ntsc?1:0);
	        	//Toast.makeText(this, "Starting...", Toast.LENGTH_SHORT);
	        	setRightMouse(mouse_button);
	        	initSDL();
	        	
	        	/*if (f1Path != null && new File(f1Path + ".asf").exists())
	        		loadState(f1Path, 0);*/
        	}
        } else {
        	tv.append("\nSelect the \"Manage\" menu item !");
        	setContentView(tv);
        }
    }
    
    public static int default_keycodes [] = {  KeyEvent.KEYCODE_P, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_O,
    	KeyEvent.KEYCODE_L, KeyEvent.KEYCODE_R, KeyEvent.KEYCODE_C, KeyEvent.KEYCODE_D,
    	KeyEvent.KEYCODE_F, KeyEvent.KEYCODE_E, KeyEvent.KEYCODE_T, KeyEvent.KEYCODE_X, KeyEvent.KEYCODE_V,
    	KeyEvent.KEYCODE_0, KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_2, KeyEvent.KEYCODE_3, KeyEvent.KEYCODE_4,
    	KeyEvent.KEYCODE_5, KeyEvent.KEYCODE_6, KeyEvent.KEYCODE_7, KeyEvent.KEYCODE_8,
    	KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_B, KeyEvent.KEYCODE_G, KeyEvent.KEYCODE_H,
    	KeyEvent.KEYCODE_I, KeyEvent.KEYCODE_J, KeyEvent.KEYCODE_K, KeyEvent.KEYCODE_M, KeyEvent.KEYCODE_N};
    public static String default_keycodes_string [] = { "Fire", "Alt.Fire" , "Left Mouse Click",
    	"Right Mouse Click", "Up", "Down", "Left",
    	"Right", "UpLeft", "UpRight", "DownLeft", "DownRight",
    	"Escape", "F1", "F2", "F3", "F4",
    	"F5", "F6", "F7", "F8", 
    	"Fire2", "Up2", "Down2", "Left2",
    	"Right2", "UpLeft2", "UpRight2", "DownLeft2", "DownRight2"};
    public static int current_keycodes [];
    
    public int [] getRealKeyCode(int keyCode) {
    	int h [] = new int [2];
    	h[0] = keyCode;
    	h[1] = 1;
    	if (joystick == 1) {
			for(int i=0;i<current_keycodes.length;i++) {
				if (keyCode == current_keycodes[i]) {
					if (default_keycodes[i] == KeyEvent.KEYCODE_DPAD_CENTER) {
						h[0] = KeyEvent.KEYCODE_P;
						return h;
					}
					if (i > 20) {
						// joystick 2
						if (i == 21)
							h[0] = default_keycodes[0];
						else
							h[0] = default_keycodes[i-18];
						h[1] = 2;
						return h;
					} else {
						h[0] = default_keycodes[i];
						return h;
					}
				}
			}
		}
    	return h;
    }

    
    protected KeyboardView theKeyboard;
	public KeyboardView getTheKeyboard() {
		return theKeyboard;
	}
	protected static int currentKeyboardLayout;
	protected Keyboard layouts [];
	
    public void initSDL()
    {
    	//nativeAudioInit(this);
    	//mAudioThread = new AudioThread(this);
        /*mGLView = new DemoGLSurfaceView(this);
        setContentView(mGLView);*/
    	if (mGLView == null)
    	setContentView(R.layout.main);
    	mGLView = ((MainSurfaceView) findViewById(R.id.mainview));
    	
    	 // Receive keyboard events
        mGLView.setFocusableInTouchMode(true);
        mGLView.setFocusable(true);
        mGLView.requestFocus();
        //PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
       // wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, Globals.ApplicationName);
       // wakeLock.acquire();
        vKeyPad = new VirtualKeypad(mGLView, this, R.drawable.dpad, R.drawable.button);
		if (mGLView.getWidth() > 0)
			vKeyPad.resize(mGLView.getWidth(), mGLView.getHeight());
		
        if (theKeyboard == null) {
        theKeyboard = (KeyboardView) findViewById(R.id.EditKeyboard01);
        layouts = new Keyboard [3];
        layouts[0] = new Keyboard(this, R.xml.joystick);
        layouts[1] = new Keyboard(this, R.xml.qwerty);
        layouts[2] = new Keyboard(this, R.xml.qwerty2);
        theKeyboard.setKeyboard(layouts[currentKeyboardLayout]);
        theKeyboard.setOnKeyboardActionListener(new theKeyboardActionListener());
        theKeyboard.setVisibility(View.INVISIBLE);
        theKeyboard.setPreviewEnabled(false);
        }
    }
    
    public void render() {
    	if (mGLView != null)
    		mGLView.requestRender();
    }

    @Override
    protected void onPause() {
        // TODO: if application pauses it's screen is messed up
       /* if( wakeLock != null )
            wakeLock.release();*/
        super.onPause();
        if( mGLView != null )
            mGLView.onPause();
    }

    @Override
    protected void onResume() {
      /*  if( wakeLock != null )
            wakeLock.acquire();*/
        super.onResume();
        if( mGLView != null )
            mGLView.onResume();
    }

    @Override
    protected void onDestroy() 
    {
      /*  if( wakeLock != null )
            wakeLock.release();*/
       
        if( mGLView != null )
            mGLView.exitApp();
        super.onStop();
        finish();
    }

	/*@Override
	public boolean onKeyDown(int keyCode, final KeyEvent event) {
		// Overrides Back key to use in our app
         if( mGLView != null )
             mGLView.onKeyDown(keyCode, event );
        return true;
     }
	
	@Override
	public boolean onKeyUp(int keyCode, final KeyEvent event) {
         if( mGLView != null )
             mGLView.onKeyUp( keyCode,event );
         return true;
     }
*/
    private MainSurfaceView mGLView = null;
   
   // private AudioThread mAudioThread = null;
   // private PowerManager.WakeLock wakeLock = null;
    private DataDownloader downloader = null;
    
    static final private int CONFIGURE_ID = Menu.FIRST +1;
    static final private int INPUT_ID = Menu.FIRST +2;
    static final private int RESET_ID = Menu.FIRST +3;
    static final private int TOUCH_ID = Menu.FIRST +4;
    static final private int LOAD_ID = Menu.FIRST +5;
    static final private int SAVE_ID = Menu.FIRST +6;
    static final private int MOUSE_ID = Menu.FIRST +7;
    static final private int QUIT_ID = Menu.FIRST +8;
    
    private SoundThread soundThread;
    
    public ByteBuffer initSound(int freq, int bits, int block_buffer_len, int nb_blocks) {
    	soundThread = new SoundThread(freq, bits, block_buffer_len, nb_blocks);
    	return soundThread.play();
    }
    
    public void playSound() {
    	if (soundThread != null)
    		soundThread.send();
    }
    
    private AudioTrack audio;
    private boolean play;
    
    public void initAudio(int freq, int bits) {
    	if (audio == null) {
    		audio = new AudioTrack(AudioManager.STREAM_MUSIC, freq, AudioFormat.CHANNEL_CONFIGURATION_MONO, bits == 8?AudioFormat.ENCODING_PCM_8BIT:AudioFormat.ENCODING_PCM_16BIT, freq==44100?32*1024:16*1024, AudioTrack.MODE_STREAM);
    		Log.i("UAE", "AudioTrack initialized: " + freq);
    		audio.play();
    	}
    }
    
    public int sendAudio(short data [], int size) {
    	if (audio != null) {
    		if (!play) {
    			play = true;
    		}
    		return audio.write(data, 0, size);
    	}
    	return -1;
    }
    
    public void pauseAudio() {
    	if (audio != null && play) {
    		audio.pause();
    		Log.i("UAE", "audio paused");
    	}
    	
    }
    
    public void playAudio() {
    	if (audio != null && play) {
    		audio.play();
    	}
    }
    
    public void stopAudio() {
    	if (audio != null && play) {
    		audio.stop();
    	}
    }
    
    private static final int SHIFT_KEYB = 150;
    
    public native void setPrefs(String rom, String floppy1, String floppy2, String floppy3, String floppy4, int frameskip, int m68k_speed, int timeslice, int change_sound, int sound, int change_disk, int reset, int drive_status, int ntsc);
    public native void saveState(String filename, int num);
    public native void loadState(String filename, int num);
    public native void nativeReset();
    public native void nativeQuit();
    public native void setRightMouse(int right);
    //public native void nativeAudioInit(DemoActivity callback);
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        // We are going to create two menus. Note that we assign them
        // unique integer IDs, labels from our string resources, and
        // given them shortcuts.
        //menu.add(0, RESET_ID, 0, R.string.reset);
        menu.add(0, CONFIGURE_ID, 0, R.string.configure);
        menu.add(0, RESET_ID, 0, R.string.reset);
        menu.add(0, QUIT_ID, 0, R.string.quit);
        if (!cyclone) {
        menu.add(0, LOAD_ID, 0, R.string.load_state);
        menu.add(0, SAVE_ID, 0, R.string.save_state);
        }
        
        menu.add(0, INPUT_ID, 0, R.string.keyb_mode);
        menu.add(0, TOUCH_ID, 0, R.string.show_touch);
        menu.add(0, MOUSE_ID, 0, R.string.change_mouse);
        
        return true;
    }
    
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
    	if (item != null) {
        switch (item.getItemId()) {
        	case CONFIGURE_ID:
        		Intent settingsIntent = new Intent();
           		settingsIntent.setClass(this, Settings.class);
           		startActivityForResult(settingsIntent, CONFIGURE_ID);
           		break;
        	case RESET_ID: 
        		nativeReset();
        		break;
        	case INPUT_ID: 
        		if (joystick == 1) {
        			joystick = 0;
        			item.setTitle(R.string.joystick_mode);
        			switchKeyboard(1, true);
        		} else if (joystick == 0) {
        			joystick = 1;
        			item.setTitle(R.string.keyb_mode);
        			switchKeyboard(0, false);
        		}
        		break;
        	case TOUCH_ID:
        		if (touch) {
        			touch = false;
        			item.setTitle(R.string.show_touch);
        			theKeyboard.setVisibility(View.INVISIBLE);
        		} else {
        			touch = true;
        			item.setTitle(R.string.hide_touch);
        			if (currentKeyboardLayout != 0)
        				theKeyboard.setVisibility(View.VISIBLE);
        		}
        		if (mGLView != null && vKeyPad == null) {
    				mGLView.shiftImage(joystick==1&&touch?SHIFT_KEYB:0);
    			}
        		break;
        	case MOUSE_ID:
        		mouse_button = 1 - mouse_button;
        		if (mouse_button == 1)
        			Toast.makeText(this, R.string.mouse_right, Toast.LENGTH_SHORT);
        		else
        			Toast.makeText(this, R.string.mouse_left, Toast.LENGTH_SHORT);
        		setRightMouse(mouse_button);
        		break;
        	case LOAD_ID:
        		if (f1Path != null)
        			loadState(f1Path, 0);
        		break;
        	case SAVE_ID:
        		if (f1Path != null)
        			saveState(f1Path, 0);
        		break;
        	case QUIT_ID:
        		nativeQuit();
        		break;
        }
    	}
        return super.onMenuItemSelected(featureId, item);
    }
    
    @Override
	public boolean onMenuOpened(int featureId, Menu menu) {
		onPause();
		return super.onMenuOpened(featureId, menu);
	}

	@Override
	public void onOptionsMenuClosed(Menu menu) {
		onResume();
		super.onOptionsMenuClosed(menu);
	}
    
class DataDownloader extends Thread
{
	class StatusWriter
	{
		private TextView Status;
		private DemoActivity Parent;

		public StatusWriter( TextView _Status, DemoActivity _Parent )
		{
			Status = _Status;
			Parent = _Parent;
		}
		
		public void setText(final String str)
		{
			class Callback implements Runnable
			{
        		public TextView Status;
	        	public String text;
	        	public void run()
    	    	{
        			Status.setText(text);
        		}
	        }
    	    Callback cb = new Callback();
    	    cb.text = new String(str);
    	    cb.Status = Status;
			Parent.runOnUiThread(cb);
		}
		
	}
	public DataDownloader( DemoActivity _Parent, TextView _Status )
	{
		Parent = _Parent;
		DownloadComplete = false;
		Status = new StatusWriter( _Status, _Parent );
		Status.setText( "Connecting to " + Globals.DataDownloadUrl );
		this.start();
	}

	@Override
	public void run() 
	{
	
		String path = getOutFilePath("DownloadFinished.flag");
		InputStream checkFile = null;
		try {
			checkFile = new FileInputStream( "/sdcard" );
		} catch( FileNotFoundException e ) {
		} catch( SecurityException e ) { };
		if( checkFile != null )
		{
			Status.setText( "No need to download" );
			DownloadComplete = true;
			initParent();
			return;
		}
		checkFile = null;
		
		// Create output directory
		if( Globals.DownloadToSdcard )
		{
			try {
				(new File( "/sdcard/" + Globals.ApplicationName )).mkdirs();
			} catch( SecurityException e ) { };
		}
		else
		{
			try {
				FileOutputStream dummy = Parent.openFileOutput( "dummy", Parent.MODE_WORLD_READABLE );
				dummy.write(0);
				dummy.flush();
			} catch( FileNotFoundException e ) {
			} catch( java.io.IOException e ) {};
		}
		
		HttpGet request = new HttpGet(Globals.DataDownloadUrl);
		request.addHeader("Accept", "*/*");
		HttpResponse response = null;
		try {
			DefaultHttpClient client = new DefaultHttpClient();
			client.getParams().setBooleanParameter("http.protocol.handle-redirects", true);
			response = client.execute(request);
		} catch (IOException e) { } ;
		if( response == null )
		{
			Status.setText( "Error connecting to " + Globals.DataDownloadUrl );
			return;
		}

		Status.setText( "Downloading data from " + Globals.DataDownloadUrl );
		
		ZipInputStream zip = null;
		try {
			zip = new ZipInputStream(response.getEntity().getContent());
		} catch( java.io.IOException e ) {
			Status.setText( "Error downloading data from " + Globals.DataDownloadUrl );
			return;
		}
		
		byte[] buf = new byte[1024];
		
		ZipEntry entry = null;

		while(true)
		{
			entry = null;
			try {
				entry = zip.getNextEntry();
			} catch( java.io.IOException e ) {
				Status.setText( "Error downloading data from " + Globals.DataDownloadUrl );
				return;
			}
			if( entry == null )
				break;
			if( entry.isDirectory() )
			{
				try {
					(new File( getOutFilePath(entry.getName()) )).mkdirs();
				} catch( SecurityException e ) { };
				continue;
			}
			
			OutputStream out = null;
			path = getOutFilePath(entry.getName());
			
			try {
				out = new FileOutputStream( path );
			} catch( FileNotFoundException e ) {
			} catch( SecurityException e ) { };
			if( out == null )
			{
				Status.setText( "Error writing to " + path );
				return;
			}

			Status.setText( "Writing file " + path );

			try {
				int len;
				while ((len = zip.read(buf)) > 0)
				{
					out.write(buf, 0, len);
				}
				out.flush();
			} catch( java.io.IOException e ) {
				Status.setText( "Error writing file " + path );
				return;
			}

		}

		OutputStream out = null;
		path = getOutFilePath("DownloadFinished.flag");
		try {
			out = new FileOutputStream( path );
			out.write(0);
			out.flush();
		} catch( FileNotFoundException e ) {
		} catch( SecurityException e ) {
		} catch( java.io.IOException e ) {
			Status.setText( "Error writing file " + path );
			return;
		};
		
		if( out == null )
		{
			Status.setText( "Error writing to " + path );
			return;
		}
	
		Status.setText( "Finished" );
		DownloadComplete = true;
		
		initParent();
	};
	
	private void initParent()
	{
		class Callback implements Runnable
		{
       		public DemoActivity Parent;
        	public void run()
   	    	{
    	    		Parent.initSDL();
       		}
        }
   	    Callback cb = new Callback();
        cb.Parent = Parent;
		Parent.runOnUiThread(cb);
	}
	
	private String getOutFilePath(final String filename)
	{
		if( Globals.DownloadToSdcard )
			return  "/sdcard/" + Globals.ApplicationName + "/" + filename;
		return Parent.getFilesDir().getAbsolutePath() + "/" + filename;
	};
	
	public boolean DownloadComplete;
	public StatusWriter Status;
	private DemoActivity Parent;
}

private int currentKeyStates = 0;


public void onGameKeyChanged(int keyStates) {
	if (mGLView != null) {
		manageKey(keyStates, VirtualKeypad.BUTTON, current_keycodes[0]);
		manageKey(keyStates, VirtualKeypad.UP, current_keycodes[4]);
		manageKey(keyStates, VirtualKeypad.DOWN, current_keycodes[5]);
		manageKey(keyStates, VirtualKeypad.LEFT, current_keycodes[6]);
		manageKey(keyStates, VirtualKeypad.RIGHT, current_keycodes[7]);
		manageKey(keyStates, VirtualKeypad.UP | VirtualKeypad.LEFT, current_keycodes[8]);
		manageKey(keyStates, VirtualKeypad.UP | VirtualKeypad.RIGHT, current_keycodes[9]);
		manageKey(keyStates, VirtualKeypad.DOWN | VirtualKeypad.LEFT, current_keycodes[10]);
		manageKey(keyStates, VirtualKeypad.DOWN | VirtualKeypad.RIGHT, current_keycodes[11]);
	}
	
	currentKeyStates = keyStates;
	
}

private void manageKey(int keyStates, int key, int press) {
	 if ((keyStates & key) == key && (currentKeyStates & key) == 0) {
		// Log.i("FC64", "down: " + press );
		 mGLView.keyDown(press);
	 } else if ((keyStates & key) == 0 && (currentKeyStates & key) == key) {
		// Log.i("FC64", "up: " + press );
		 mGLView.keyUp(press);
	 }
}

@Override
public boolean onKeyDown(int keyCode, KeyEvent event) {
	if (mGLView != null)
		return mGLView.keyDown(keyCode);
	return super.onKeyDown(keyCode, event);
}

@Override
public boolean onKeyUp(int keyCode, KeyEvent event) {
	if (mGLView != null)
		return mGLView.keyUp(keyCode);
	return super.onKeyUp(keyCode, event);
}

@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	
	 if (resultCode == RESULT_OK) {
		 if (requestCode == CONFIGURE_ID) {
			 onPause();
			 showDialog(1); 
		 }
	 }
}

@Override
protected Dialog onCreateDialog(int id) {
	
		switch (id) {
	   case 1: return new AlertDialog.Builder(DemoActivity.this)
       .setTitle(R.string.reset)
       .setMessage(R.string.reset_ask)
       .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
           public void onClick(DialogInterface dialog, int whichButton) {
        	   checkConf();
        	   checkFiles(true);
        	   onResume();
           }
       })
       .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
           public void onClick(DialogInterface dialog, int whichButton) {
        	  
        	   checkConf();
        	   checkFiles(false);
        	  onResume();
           }
       })
       .create();
	   case 2: return new AlertDialog.Builder(DemoActivity.this)
       .setTitle(R.string.quit)
       .setMessage(R.string.quit_info)
       .setPositiveButton(R.string.quit, new DialogInterface.OnClickListener() {
           public void onClick(DialogInterface dialog, int whichButton) {
        	   nativeQuit();
           }
       })
       .create();
	   case 3: return new AlertDialog.Builder(DemoActivity.this)
       .setTitle(R.string.notes)
       .setMessage(R.string.release_notes_097)
       .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
           public void onClick(DialogInterface dialog, int whichButton) {
        	   onResume();
           }
       })
       
       .create();
		}
		return null;
}
}
