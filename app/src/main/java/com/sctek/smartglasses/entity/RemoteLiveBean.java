package com.sctek.smartglasses.entity;

public class RemoteLiveBean implements Cloneable {

	private long _id = 0;

	private String uid = "";
	private String name = "";
	private String pwd = "";
	private long time = 0L;
	private int clearity = 0;

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	public long get_id() {
		return _id;
	}

	public void set_id(long l) {
		this._id = l;
	}

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPwd() {
		return pwd;
	}

	public void setPwd(String pwd) {
		this.pwd = pwd;
	}

	public int getClearity() {
		return clearity;
	}

	public void setClearity(int clearity) {
		this.clearity = clearity;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}
}
