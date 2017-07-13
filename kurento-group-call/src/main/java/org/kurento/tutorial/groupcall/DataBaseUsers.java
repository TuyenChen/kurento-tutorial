package org.kurento.tutorial.groupcall;

import java.io.IOException;
import java.util.ArrayList;

public class DataBaseUsers {
	public final ArrayList<Account> list;

	public DataBaseUsers () {
		this.list = new ArrayList<>();
		Account ac1 = new Account("pvhai", "123", "teacher");
		Account ac2 = new Account("tuyen", "123", "student");
		Account ac3 = new Account("dung", "123", "student");
		Account ac4 = new Account("nam", "123", "student");
		Account ac5 = new Account("huy", "123", "student");
		Account ac6 = new Account("phat", "123", "student");
		this.list.add(ac1);
		this.list.add(ac2);
		this.list.add(ac3);
		this.list.add(ac4);
		this.list.add(ac5);
		this.list.add(ac6);
	}
	public String getRoleByName(String name) {
		for (int i=0; i<this.list.size(); i++) {
			Account tempAcc = this.list.get(i);
			if(tempAcc.name.equals(name)) {
				return tempAcc.role;
			}
		}
		return "student";
	}
	public boolean checkLogin(String name, String password) {
		for (int i=0; i<this.list.size(); i++) {
			Account tempAcc = this.list.get(i);
			if(tempAcc.name.equals(name) && tempAcc.password.equals(password)) {
				return true;
			}
		}
		return false;
	}
	class Account {
		public String name;
		public String role;
		public String password;
		public Account (String name, String password, String role) {
			this.name = name;
			this.password = password;
			this.role = role;
		}
	}
}