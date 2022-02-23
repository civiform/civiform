# Make sure the bastion is within the vnet of the database
resource "azurerm_subnet" "bastion_subnet" {
  name                 = "bastion_subnet"
  resource_group_name  = var.resource_group_name
  virtual_network_name = var.vnet_name
  address_prefixes     = var.bastion_address_prefixes
}

# Create network security group and SSH rule for public subnet.
resource "azurerm_network_security_group" "public_nsg" {
  name                = "${var.resource_group_name}-pblc-nsg"
  location            = var.resource_group_location
  resource_group_name = var.resource_group_name

  # deny the access to the machine
  # to access: manually run script to allow just your IP 
  # (see db-connection script for an example of how to do this)
  security_rule {
    name                       = "ssh-rule"
    priority                   = 100
    direction                  = "Inbound"
    access                     = "Deny"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "22"
    source_address_prefix      = "*"
    destination_address_prefix = "VirtualNetwork"
  }
}

# Associate network security group with public subnet.
resource "azurerm_subnet_network_security_group_association" "bastion_subnet_assoc" {
  subnet_id                 = azurerm_subnet.bastion_subnet.id
  network_security_group_id = azurerm_network_security_group.public_nsg.id
}

# Create a public IP address for bastion host VM in public subnet.
resource "azurerm_public_ip" "public_ip" {
  name                = "${var.resource_group_name}-ip"
  location            = var.resource_group_location
  resource_group_name = var.resource_group_name
  allocation_method   = "Dynamic"
}
# prevent all access to the bastion's public IP address, but then in script 
# set it up so that the current machine can access the public ip 
# remove this set up 

# Create network interface for bastion host VM in public subnet.
resource "azurerm_network_interface" "bastion_nic" {
  name                = "${var.resource_group_name}-bstn-nic"
  location            = var.resource_group_location
  resource_group_name = var.resource_group_name

  ip_configuration {
    name                          = "${var.resource_group_name}-bstn-nic-cfg"
    subnet_id                     = azurerm_subnet.bastion_subnet.id
    private_ip_address_allocation = "Dynamic"
    public_ip_address_id          = azurerm_public_ip.public_ip.id
  }
}

resource "azurerm_network_interface_security_group_association" "nic_sg" {
  network_interface_id      = azurerm_network_interface.bastion_nic.id
  network_security_group_id = azurerm_network_security_group.public_nsg.id
}

# Create bastion host VM.
resource "azurerm_linux_virtual_machine" "bastion_vm" {
  name                  = "${var.resource_group_name}-bstn-vm"
  location              = var.resource_group_location
  resource_group_name   = var.resource_group_name
  network_interface_ids = ["${azurerm_network_interface.bastion_nic.id}"]
  size                  = "Standard_B1ls"
  admin_username        = "adminuser"

  # this is required, but we deny all ingres to the machine
  admin_ssh_key {
    username   = "adminuser"
    public_key = file("~/.ssh/bastion.pub")
  }

  os_disk {
    caching              = "ReadWrite"
    storage_account_type = "Standard_LRS"
  }

  source_image_reference {
    publisher = "Canonical"
    offer     = "UbuntuServer"
    sku       = "18.04-LTS"
    version   = "latest"
  }
}
