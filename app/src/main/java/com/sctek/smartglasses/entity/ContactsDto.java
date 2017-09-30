package com.sctek.smartglasses.entity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ContactsDto implements Cloneable {

        private long _id = 0;

        private String displayName = "";

        private List<ContactsDataDto> contactsDataDtoList = new ArrayList<ContactsDataDto>();

        @Override
        public Object clone() throws CloneNotSupportedException {
                ContactsDto contactsDto = (ContactsDto) super.clone();
                List<ContactsDataDto> dataDtoList = new ArrayList<ContactsDataDto>();
                List<ContactsDataDto> tmpList = contactsDto.getContactsDataDtoList();
                for (Iterator<ContactsDataDto> it = tmpList.iterator(); it.hasNext(); ) {
                        dataDtoList.add((ContactsDataDto) it.next().clone());
                }
                contactsDto.setContactsDataDtoList(dataDtoList);
                return contactsDto;
        }

        public long get_id() {
                return _id;
        }

        public void set_id(long _id) {
                this._id = _id;
        }

        public String getDisplayName() {
                return displayName;
        }

        public void setDisplayName(String displayName) {
                this.displayName = displayName;
        }

        public List<ContactsDataDto> getContactsDataDtoList() {
                return contactsDataDtoList;
        }

        public void setContactsDataDtoList(List<ContactsDataDto> contactsDataDtoList) {
                this.contactsDataDtoList = contactsDataDtoList;
        }
}
