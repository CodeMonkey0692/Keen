package keen.metaParsing.Music;
import keen.metaParsing.ChainedBlobstoreInputStream;

import java.util.Map;
import java.util.HashMap;

import java.io.IOException;
import java.lang.NullPointerException;

public class Mp3ID3v2 implements MusicParser {

	private boolean err;
	private int remAttribNum;
	private Map<String,byte[]> attribs;


	public Mp3ID3v2(ChainedBlobstoreInputStream tag) throws IOException {
		// check if id3
		byte[] header = new byte[10];
		if (tag.read(header) != 10) {
			err = true;
			return;
		}

		// check for "ID3" and version number
		if (!(header[0] == (byte)(0x49)
			&& header[1] == (byte)(0x44)
			&& header[2] == (byte)(0x33)
			&& header[3] == (byte)(0x03)
			&& header[4] == (byte)(0x00))) {
			err = true;
			return;
		}

		checkFlags(header,5);
		int tagSize = getSize(header,6);
		// attributes to parse
		attribs = new HashMap<String,byte[]>();
		// Album
		attribs.put("TALB",null);
		// Title
		attribs.put("TIT2",null);
		// Genre
		attribs.put("TCON",null);
		// Artist
		attribs.put("TPE1",null);
		// Track Number
		attribs.put("TRCK",null);
		// Disc Number
		attribs.put("TPA",null);
		// Rating
		attribs.put("POPM",null);
		// Length
		//attribs.put("TLEN",null);
		remAttribNum = attribs.size();

		while (remAttribNum != 0 && tag.getOffset() < tagSize) {
			parseFrame(tag);
		}
		tag.close();
		err = false;

	}

	private void checkFlags(byte[] header, int offset) {

	}

	private int getSize(byte[] header, int offset) {
		return (((int)(header[offset] & 0xFF) << 21) | ((int)(header[offset + 1] & 0xFF) << 14) | ((int)(header[offset + 2] & 0xFF) << 7) | (int)(header[offset+3] & 0xFF));
	}

	private void parseFrame(ChainedBlobstoreInputStream tag) throws IOException {
		//----- Begin frame header reading
		byte[] buffer = new byte[4];
		tag.read(buffer);
		
		String frameID = new String(buffer);

		tag.read(buffer);

		//HACK
		// this is unsigned. and java has no unsigned int
		long size = (((int)(buffer[0] & 0xFF) << 24) | ((int)(buffer[1] & 0xFF) << 16) | ((int)(buffer[2] & 0xFF) << 8) | ((int)(buffer[3] & 0xFF)));

		// may not use
		byte[] flags = new byte[2];
		tag.read(flags);
		//----- End frame header reading

		if (attribs.containsKey(frameID)) {
			--remAttribNum;
			buffer = new byte[(int)(size)];
			tag.read(buffer);
			attribs.put(frameID,buffer);
			// do something
		} else {
			// not needed
			tag.skip(size);
		}
	}


	public boolean success() {
		return !err;
	}

	// Note all text incoded strings start with a byte to indicate charset
	// 0x00 = ISO-8859-1
	// 0x0.1 = Unicode 2.0 these strings also start with a BOM to indicate byte order 
	// LE? 0xFF 0xFE
	// BE? 0xFE 0xFF

	public String getAlbum() {
		try {
			return new String(attribs.get("TALB"),1,attribs.get("TALB").length -1);
		} catch (NullPointerException e) {
			return "";
		}
	}

	public int getRating(){
		// POPM consits of a null terminated <email string> a rating byte and a optional 
		// counter bytes
		int i = 0;
		byte[] data;
		if ((data = attribs.get("POPM")) == null)
			return 0;
		// don't care about the email string
		while (data[i++] != 0x00);
		// return the unsigned counter byte
		return (int)(data[i] & 0xFF);

	}

	public String getSongName() {
		try {
			return new String(attribs.get("TIT2"),1,attribs.get("TIT2").length -1);
		} catch (NullPointerException e) {
			return "";
		}

	}

	public String getGenre() {
		try {
			return new String(attribs.get("TCON"),1,attribs.get("TCON").length -1);
		} catch (NullPointerException e) {
			return "";
		}

	}


	public String getArtist() {
		try {
			return new String(attribs.get("TPE1"),1,attribs.get("TPE1").length -1);
		} catch (NullPointerException e) {
			return "";
		}
	}

	public String getTrackNum() {
		try {
			return new String(attribs.get("TRCK"),1,attribs.get("TRCK").length -1);
		} catch (NullPointerException e) {
			return "";
		}

	}

	public String getDiscNum() {
		try {
			return new String(attribs.get("TPA"),1,attribs.get("TPA").length -1);
		} catch (NullPointerException e) {
			return "";
		}
	}


	/*
	public String getLength() {
		return new String(attribs.get("TLEN"));
	}
	*/

}
