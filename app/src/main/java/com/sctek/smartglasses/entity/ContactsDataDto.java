package com.sctek.smartglasses.entity;

public class ContactsDataDto implements Cloneable {

        private long _id = 0;

        private long contactId = -1;

        private String type = "";

        private String data = "";

        @Override
        public Object clone() throws CloneNotSupportedException {
                return super.clone();
        }

        public long get_id() {
                return _id;
        }

        public void set_id(long _id) {
                this._id = _id;
        }

        public long getContactId() {
                return contactId;
        }

        public void setContactId(long contactId) {
                this.contactId = contactId;
        }

        public String getType() {
                return type;
        }

        public void setType(String type) {
                this.type = type;
        }

        public String getData() {
                return data;
        }

        public void setData(String data) {
                this.data = data;
        }
}
