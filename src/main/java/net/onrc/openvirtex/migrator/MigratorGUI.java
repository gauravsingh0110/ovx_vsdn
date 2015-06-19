package net.onrc.openvirtex.migrator;

//TODO Outdated class needs lots of updates not using anymore

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import net.onrc.openvirtex.elements.host.Host;
import net.onrc.openvirtex.elements.port.OVXPort;





public class MigratorGUI extends JFrame {
	
	private JLabel hId,hMAC,sVId,sPId,tId,oPId,nPId;
	private JTextField hIdt,hMACt,sVIdt,sPIdt,tIdt,oPIdt,nPIdt;
	private OVXPort ovxPort;
	private Host host;
	private VSDNMigrator vsdnm=new VSDNMigrator();
	
	public void showMigrator()
	{
		
		hId=new JLabel();
		hId.setText("Host ID");
		hIdt=new JTextField();
		hIdt.setColumns(20);
		
		hMAC=new JLabel();
		hMAC.setText("Host MAC");
		hMACt=new JTextField();
		hMACt.setColumns(20);   
		
		sVId=new JLabel();
		sVId.setText("Switch Virtual dpid");
		sVIdt=new JTextField();
		sVIdt.setColumns(20);
		
		sPId=new JLabel();
		sPId.setText("Switch Physical dpid");
		sPIdt=new JTextField();
		sPIdt.setColumns(20);
		
		tId=new JLabel();
		tId.setText("Tenant ID");
		tIdt=new JTextField();
		tIdt.setColumns(20);
		
		oPId=new JLabel();
		oPId.setText("Old Port");
		oPIdt=new JTextField();
		oPIdt.setColumns(20);
		

		nPId=new JLabel();
		nPId.setText("New Port");
		nPIdt=new JTextField();
		nPIdt.setColumns(20);
		
				
		
		JButton but=new JButton();
		but.setText("Migrate");
		add(hId);
		add(hIdt);
        add(tId);
		add(tIdt);
		add(hMAC);
		add(hMACt);
		add(sVId);
		add(sVIdt);
		add(sPId);
		add(sPIdt);
		//add(oPId);
		//add(oPIdt);
		add(nPId);
		add(nPIdt);
		add(but);
		but.addActionListener(new ActionListener(){
					public void actionPerformed(
							ActionEvent e){
											if(performCheck()){
												vsdnm.openVirtualNetwork(Integer.parseInt(tIdt.getText()));
												vsdnm.disconnectHost(Integer.parseInt(hIdt.getText()));
												ovxPort=vsdnm.createPort(Long.parseLong(sPIdt.getText().replace(":", ""),16), Short.parseShort(nPIdt.getText()));
												if(ovxPort != null){
													host=vsdnm.connectHost(Long.parseLong(sVIdt.getText().replace(":", ""),16),ovxPort.getPortNumber(), hMACt.getText());
													
												}
												if(ovxPort != null && host != null){
													JOptionPane.showMessageDialog(null, "Successfully Migrated the Host");
												}
												else{
													JOptionPane.showMessageDialog(null, "Some error occured, check Logs");
												}
											}
											else{
												JOptionPane.showMessageDialog(null, "Please input all the fields");
											}
											
										  }
									}
							);
		//setLayout(new FlowLayout());
		setLayout(new GridLayout(7, 2));
		setSize(250,250);
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		this.setLocation(dim.width/2-this.getSize().width/2, dim.height/2-this.getSize().height/2);
		
		setVisible(true);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
	}
	
	private boolean performCheck(){
		//hId,hMAC,sVId,sPId,tId,oPId,nPId;
		if(hIdt.getText()=="")
			return false;
		if(hMACt.getText()=="")
			return false;
		if(sVIdt.getText()=="")
			return false;
		if(sPIdt.getText()=="")
			return false;
		if(tIdt.getText()=="")
			return false;
		if(nPIdt.getText()=="")
			return false;

		return true;
	}
	

}
