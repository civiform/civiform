# create a public IP for so we can ssh to the bastion
resource "azurerm_public_ip" "bastion_pip" {
  name                = "bastion_pip"
  location            = var.resource_group_location
  resource_group_name = var.resource_group_name
  allocation_method   = "Static"
  sku                 = "Standard"
}

# Make sure the bastion is within the vnet of the database
resource "azurerm_subnet" "bastion_subnet" {
  name                 = "bastion_subnet"
  resource_group_name  = var.resource_group_name
  virtual_network_name = var.vnet_name
  address_prefixes = [
    "10.0.6.0/24"
  ]
}

# Create network security group and SSH rule for public subnet.
resource "azurerm_network_security_group" "public_nsg" {
  name                = "${var.resource_group_name}-pblc-nsg"
  location            = var.resource_group_location
  resource_group_name = var.resource_group_name

  # Allow SSH traffic in from Internet to public subnet.
  security_rule {
    name                       = "allow-ssh-all"
    priority                   = 100
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "22"
    source_address_prefix      = "*"
    destination_address_prefix = "*"
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
  name                  = "${var.resource_group_name}-bstn-vm001"
  location              = var.resource_group_location
  resource_group_name   = var.resource_group_name
  network_interface_ids = ["${azurerm_network_interface.bastion_nic.id}"]
  size                  = "Standard_B1ls"
  admin_username        = "adminuser"

  os_disk {
    caching              = "ReadWrite"
    storage_account_type = "Standard_LRS"
  }

  source_image_reference {
    publisher = "Canonical"
    offer     = "UbuntuServer"
    sku       = "16.04-LTS"
    version   = "latest"
  }

  admin_ssh_key {
    username = "adminuser"
    # ssh-keygen -t rsa -b 4096 -C "sgoldblatt@google.com" -f $HOME/.ssh/bastion
    public_key = file("~/.ssh/bastion.pub")
  }
}
