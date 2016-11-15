package com.nadeen.emu.chip8;

import android.app.*;
import android.os.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.concurrent.*;
import android.widget.*;
import android.view.*;
import java.util.*;
import android.graphics.*;
import java.nio.*;

public class MainActivity extends Activity 
{
	private TextView tv;
	private ScrollView sv;
	private ImageView iv;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
		tv = (TextView)findViewById(R.id.mainTextView);
		sv = (ScrollView)findViewById(R.id.mainScrollView);
		iv = (ImageView)findViewById(R.id.mainImageView);
		iv.setTop(0);
		tv.setText("\n\n\n\n\n");
		new Cpu().start();
    }
	public void updatebmp(Bitmap bmp)
	{
		final Bitmap bmp2 = bmp;
		runOnUiThread(new Runnable(){
				@Override
				public void run()
				{
					iv.setImageBitmap(bmp2);
				}
			});
	}
	void out(String str)
	{
		final String str2 = str;
		runOnUiThread(new Runnable(){
				@Override
				public void run()
				{
					tv.setText(tv.getText().subSequence(0,tv.getText().length()-5)+str2+"\n\n\n\n\n");
					sv.fullScroll(View.FOCUS_DOWN);
				}
			});
	}
	public void out(Exception e)
	{
		StringWriter out = new StringWriter();
		e.printStackTrace(new PrintWriter(out));
		out.flush();
		out(out.toString());
		try
		{
			out.close();
		}
		catch (Exception e2)
		{}
	}
	class Cpu extends Thread
	{
		Chip8 cpu;
		@Override
		public void run()
		{
			super.run();
			try
			{
				out("loading.....\n");
				cpu = new Chip8();
				cpu.initialize();
				cpu.load("/sdcard/c8/maze");
				Thread.sleep(1000);
			}
			catch (Exception e){out(e);}
			while(true)
			{
				try
				{
					Thread.sleep(50);
					cpu.emulate();
				}
				catch (Exception e)
				{out(e);}
			}
		}
	}
	class Chip8
	{
		// http://www.multigesture.net/articles/how-to-write-an-emulator-chip-8-interpreter/
		short opcode;
		short I;
		short pc;
		short sp;
		short[] stack;
		byte delay_timer;
		byte sound_timer;
		byte[] memory;
		byte[] vram;
		byte[] V;
		byte[] key;
		byte[] chip8_fontset;
		Random random;
		Bitmap bmp;
		public Chip8() throws Exception
		{
			random = new Random();
			chip8_fontset = new byte[80];
			/*{ 
			 0xF0, 0x90, 0x90, 0x90, 0xF0, // 0
			 0x20, 0x60, 0x20, 0x20, 0x70, // 1
			 0xF0, 0x10, 0xF0, 0x80, 0xF0, // 2
			 0xF0, 0x10, 0xF0, 0x10, 0xF0, // 3
			 0x90, 0x90, 0xF0, 0x10, 0x10, // 4
			 0xF0, 0x80, 0xF0, 0x10, 0xF0, // 5
			 0xF0, 0x80, 0xF0, 0x90, 0xF0, // 6
			 0xF0, 0x10, 0x20, 0x40, 0x40, // 7
			 0xF0, 0x90, 0xF0, 0x90, 0xF0, // 8
			 0xF0, 0x90, 0xF0, 0x10, 0xF0, // 9
			 0xF0, 0x90, 0xF0, 0x90, 0x90, // A
			 0xE0, 0x90, 0xE0, 0x90, 0xE0, // B
			 0xF0, 0x80, 0x80, 0x80, 0xF0, // C
			 0xE0, 0x90, 0x90, 0x90, 0xE0, // D
			 0xF0, 0x80, 0xF0, 0x80, 0xF0, // E
			 0xF0, 0x80, 0xF0, 0x80, 0x80  // F
			 };*/
		}
		void initialize() throws Exception
		{
			pc     = 0x200;  // Program counter starts at 0x200
			opcode = 0;      // Reset current opcode	
			I      = 0;      // Reset index register
			sp     = 0;      // Reset stack pointer
			key = new byte[16];
			vram = new byte[64*32];
			stack = new short[16];
			V = new byte[16];
			memory = new byte[4096];
			// Load fontset
			for(int i = 0; i < 80; ++i)
				memory[i] = chip8_fontset[i];		
			delay_timer = 0;
			sound_timer = 0;
			bmp = Bitmap.createBitmap(64,32,Bitmap.Config.ARGB_8888);
			update();
		}
		void load(String file) throws Exception
		{
			InputStream f = new FileInputStream(file);
			int size = f.available();
			byte[] buffer = new byte[size];
			f.read(buffer);
			for(int i = 0; i < size; ++i)
				memory[i + 512] = buffer[i];
		}
		void emulate() throws Exception
		{
			opcode = (short)((int)memory[pc] << 8 | (int)memory[pc + 1]);
			exec();
			pc += 2;
			if(delay_timer > 0)
				delay_timer--;
			if(sound_timer > 0)
			{
				if(sound_timer == 1)
					debug("BEEP!\n");
				sound_timer--;
			}
		}
		// https://en.m.wikipedia.org/wiki/CHIP-8
		private void exec() throws Exception
		{
			short src = 0;
			short dest = 0;
			switch(opcode & 0xF000)
			{
				case 0x8000:
				{
					switch(opcode & 0x000F)
					{
						case 0x0000:
							byte src2 = (byte)((opcode & 0x00F0) >> 4);
							src = V[src2];
							byte dest2 = (byte)((opcode & 0x0F00) >> 8);
							dest = V[dest2];
							V[dest2] = (byte)src;
							debug("V%X(0x%02X) = V%X(0x%02X)\n",dest2,dest,src2,src);
							break;
						default:
							undefined();
					}
					break;
				}
				case 0x1000:
				{
					pc = (short)(opcode & 0x0FFF);
					debug("jmp 0x%03X\n",pc);
					break;
				}
				case 0x7000:
				{
					src = (byte)(opcode & 0x00FF);
					dest = (byte)((opcode & 0x0F00) >> 8);
					byte src2 = V[dest];
					byte result = (byte)(src2+src);
					V[dest] = result;
					debug("V%X(0x%02X) += 0x%02X = 0x%02X)\n",dest,src2,src,result);
					break;
				}
				case 0xD000:
					{
						short x = V[(opcode & 0x0F00) >> 8];
						short y = V[(opcode & 0x00F0) >> 4];
						short height = (short)(opcode & 0x000F);
						short pixel;
						V[0x0F] = 0;
						for (int yline = 0; yline < height; yline++)
						{
							pixel = memory[I + yline];
							for(int xline = 0; xline < 8; xline++)
							{
								if((pixel & (0x80 >> xline)) != 0)
								{
									if(vram[(x + xline + ((y + yline) * 64))] == 1)
									{
										V[0x0F] = 1;                                    
									}
									vram[x + xline + ((y + yline) * 64)] ^= 1;
								}
							}
						}
						update();
						debug("vram[%d,%d]*%d and V[0x0F] == %b)\n",x,y,height,V[0x0F]);
						break;
					}
				case 0x3000:
					{
						byte src2 = (byte)((opcode & 0x0F00) >> 8);
						src = V[src2];
						dest = (byte)(opcode & 0x00FF);
						boolean result = src == dest;
						if(result)
							pc += 2;
						debug("if(V%X(%02X) == 0x%02X == %b)jmp $+2\n",src2,src,dest,result);
						break;
					}
				case 0xC000:
					{
						src = (byte)(opcode & 0x00FF);
						dest = (byte)((opcode & 0x0F00) >> 8);
						byte rand = (byte)random.nextInt(256);
						byte result = (byte)(rand & src);
						V[dest] = result;
						debug("V%X = 0x%02X & 0x%02X = 0x%02X\n",dest,rand,src,result);
						break;
					}
				case 0xA000:
					{
						src = (short)(opcode & 0x0FFF);
						I = src;
						debug("I = 0x%03X\n",src);
						break;
					}
				case 0x6000:
					{
						src =  (byte)(opcode & 0x00FF);
						dest = (byte)((opcode & 0x0F00) >> 8);
						V[dest] = (byte)src;
						debug("V%X = 0x%02X\n",dest,src);
						break;
					}
				case 0xF000:
					switch(opcode & 0x00FF)
					{
						case 0x0015:
							src =  (byte)(opcode & 0x0F00);
							delay_timer = (byte)src;
							debug("delay_timer = 0x%02X\n",src);
							break;
						default:
							undefined();
							break;
					}
					break;
				default:
					undefined();
			}
		}
		private void update()
		{
			for(int x = 0;x < 64;x++)
				for(int y = 0;y < 32;y++)
				{
					int c = Color.BLACK;
					if(vram[x+(y*32)] == 0)
						c = Color.WHITE;
					bmp.setPixel(x,y,c);
				}
			updatebmp(bmp);
		}
		private void undefined() throws Exception
		{
			debug("Unknown opcode at 0x%04X 0x%04X\n",pc,opcode);
			Thread.sleep(10000);
		}
		void debug(String fmt,Object...args) throws Exception
		{
			out(String.format(fmt, args));
		}
	}
}
