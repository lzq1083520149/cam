package com.cn.zhongdun110.camlog.contactslite;

import cn.ingenic.glasssync.services.SyncModule;
import cn.ingenic.glasssync.services.mid.MidSrcContentProvider;


public class ContactsLiteMidSrcContentProvider extends MidSrcContentProvider {

	@Override
	public SyncModule getSyncModule() {
		return ContactsLiteModule.getInstance(
				getContext().getApplicationContext());
	}

}
