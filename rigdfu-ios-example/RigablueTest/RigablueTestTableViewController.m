//
//  RigablueTestTableViewController.m
//  RigablueTest
//
//  Created by Eric Stutzenberger on 4/22/14.
//  Copyright (c) 2014 Rigado, LLC. All rights reserved.
//

#import "RigablueTestTableViewController.h"
#import "RigablueTestDeviceViewController.h"
#import "Rigablue/RigDeviceRequest.h"
#import "Rigablue/RigLeConnectionManager.h"
#import "Rigablue/RigLeBaseDevice.h"

//#define LOG_MFG_DATA
#define LUMENPLAY_SERVICE_ID        @"0f9b"
#define RIGDFU_SERVICE_ID           @"00001530-1212-efde-1523-785feabcd123"

#define VIAWEAR_SERVICE             @"2995d87f-8c0e-a894-eb4b-5407c5b416a8"

@interface RigablueTestTableViewController () <RigLeDiscoveryManagerDelegate, RigLeConnectionManagerDelegate, RigLeBaseDeviceDelegate>
{
    NSMutableArray *connectedDevices;
}
@end

@implementation RigablueTestTableViewController

- (id)initWithStyle:(UITableViewStyle)style
{
    self = [super initWithStyle:style];
    if (self) {
        // Custom initialization
    }
    return self;
}

- (void)viewDidLoad
{
    [super viewDidLoad];
    
    // Uncomment the following line to preserve selection between presentations.
    // self.clearsSelectionOnViewWillAppear = NO;
    
    // Uncomment the following line to display an Edit button in the navigation bar for this view controller.
    // self.navigationItem.rightBarButtonItem = self.editButtonItem;
    connectedDevices = [[NSMutableArray alloc] initWithCapacity:10];
    [[RigLeDiscoveryManager sharedInstance] startLeInterface];
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (IBAction)didPressDiscoveryButton:(id)sender
{
    if ([self.navigationItem.leftBarButtonItem.title isEqualToString:@"Start Discovery"]) {
        RigDeviceRequest *request;
        CBUUID *lumenplayUuid = [CBUUID UUIDWithString:LUMENPLAY_SERVICE_ID];
        CBUUID *rigdfuUuid = [CBUUID UUIDWithString:RIGDFU_SERVICE_ID];
        CBUUID *viawearUuid = [CBUUID UUIDWithString:VIAWEAR_SERVICE];
        
        NSArray			*uuidArray	= [NSArray arrayWithObjects:lumenplayUuid, rigdfuUuid, viawearUuid, nil];
        self.navigationItem.leftBarButtonItem.title = @"Stop Discovery";
        request = [RigDeviceRequest deviceRequestWithUuidList:uuidArray timeout:10.0 delegate:self allowDuplicates:YES];
        [[RigLeDiscoveryManager sharedInstance] discoverDevices:request];
    }
    else
    {
        self.navigationItem.leftBarButtonItem.title = @"Start Discovery";
        [[RigLeDiscoveryManager sharedInstance] stopDiscoveringDevices];
    }
}

- (IBAction)didPressClearButton:(id)sender
{
    [[RigLeDiscoveryManager sharedInstance] clearDiscoveredDevices];
    [self.tableView reloadData];
}

#pragma mark - Table view data source

- (NSInteger)numberOfSectionsInTableView:(UITableView *)tableView
{
    return 2;
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section
{
    if (section == 0) {
        return connectedDevices.count;
    } else {
        return [[RigLeDiscoveryManager sharedInstance] retrieveDiscoveredDevices].count;
    }
}


- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath
{
    UITableViewCell *cell = [tableView dequeueReusableCellWithIdentifier:@"AvailableDeviceCell" forIndexPath:indexPath];
    
    if (indexPath.section == 0) {
        RigLeBaseDevice *device = connectedDevices[indexPath.row];
        cell.textLabel.text = device.peripheral.name;
        if (device.isDiscoveryComplete) {
            cell.detailTextLabel.text = @"Discovery Complete";
            cell.accessoryType = UITableViewCellAccessoryDetailDisclosureButton;
        } else {
            cell.detailTextLabel.text = @"Connected";
            device.delegate = self;
            [device runDiscovery];
        }
    } else {
        RigAvailableDeviceData *availableDevice = [[RigLeDiscoveryManager sharedInstance] retrieveDiscoveredDevices][indexPath.row];
        if (availableDevice.advertisementData == nil) {
            cell.textLabel.text = availableDevice.peripheral.name;
        } else {
            cell.textLabel.text = availableDevice.advertisementData[@"kCBAdvDataLocalName"];
        }
#ifdef LOG_MFG_DATA
        NSLog(@"%@", availableDevice.advertisementData[CBAdvertisementDataManufacturerDataKey]);
#endif
        cell.detailTextLabel.text = [NSString stringWithFormat:@"RSSI: %d", availableDevice.rssi.intValue];
        cell.accessoryType = UITableViewCellAccessoryNone;
    }
    return cell;
}

- (void) tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath
{
	if (indexPath.section == 0) {
        [self.tableView deselectRowAtIndexPath:indexPath animated:YES];
        //[[RigLeConnectionManager sharedInstance] disconnectDevice:[connectedDevices objectAtIndex:indexPath.row]];
    } else if (indexPath.section == 1) {
        /* For now, stop discovery since continuation of discovery seems to cause issues */
        [[RigLeDiscoveryManager sharedInstance] stopDiscoveringDevices];
        [self.tableView deselectRowAtIndexPath:indexPath animated:YES];
        RigAvailableDeviceData *availableDevice = [[RigLeDiscoveryManager sharedInstance] retrieveDiscoveredDevices][indexPath.row];
        RigLeConnectionManager *cm = [RigLeConnectionManager sharedInstance];
        cm.delegate = self;
        [[RigLeConnectionManager sharedInstance] connectDevice:availableDevice connectionTimeout:5.0];
    }
}


// Override to support conditional editing of the table view.
- (BOOL)tableView:(UITableView *)tableView canEditRowAtIndexPath:(NSIndexPath *)indexPath
{
    // Return NO if you do not want the specified item to be editable.
    if (indexPath.section == 0) {
        return YES;
    }
    return NO;
}



// Override to support editing the table view.
- (void)tableView:(UITableView *)tableView commitEditingStyle:(UITableViewCellEditingStyle)editingStyle forRowAtIndexPath:(NSIndexPath *)indexPath
{
    if (editingStyle == UITableViewCellEditingStyleDelete) {
        // Delete the row from the data source
        [[RigLeConnectionManager sharedInstance] disconnectDevice:[connectedDevices objectAtIndex:indexPath.row]];
    }
}

- (NSString *)tableView:(UITableView *)tableView titleForDeleteConfirmationButtonForRowAtIndexPath:(NSIndexPath *)indexPath
{
    return @"Disconnect";
}

#pragma mark - Navigation

// In a storyboard-based application, you will often want to do a little preparation before navigation
- (void)prepareForSegue:(UIStoryboardSegue *)segue sender:(id)sender
{
    RigLeBaseDevice *device;
    // Get the new view controller using [segue destinationViewController].
    // Pass the selected object to the new view controller.
    RigablueTestDeviceViewController *vc = [segue destinationViewController];
    device = [connectedDevices objectAtIndex:[self.tableView indexPathForSelectedRow].row];
    if (device.name != nil) {
        vc.title = device.name;
    } else {
        vc.title = @"Unknown";
    }
    [vc setDevice:device];
}

#pragma mark
#pragma mark - RigDiscoveryManagerDelegate methods
- (void)didDiscoverDevice:(RigAvailableDeviceData *)device
{
    [self performSelectorOnMainThread:@selector(reloadTableData:) withObject:device waitUntilDone:YES];
}

- (void)reloadTableData:(NSObject*)object
{
        [self.tableView reloadData];
}

- (void)discoveryDidTimeout
{
    NSString *title     = @"Searching Complete";
    NSString *message   = @"Search session has finished.  Tap Search Lumenplay to being a new search session.";
    UIAlertView *updateCompleteAlertView = [[UIAlertView alloc] initWithTitle:title message:message delegate:self cancelButtonTitle:@"OK" otherButtonTitles:nil];
    [updateCompleteAlertView show];
    
    self.navigationItem.leftBarButtonItem.title = @"Start Discovery";
}

#pragma mark
#pragma mark - RigLeConnectionManagerDelegate methods
- (void)didConnectDevice:(RigLeBaseDevice *)device
{
    [connectedDevices addObject:device];
    [self performSelectorOnMainThread:@selector(reloadTableData:) withObject:device waitUntilDone:YES];
}

- (void)didDisconnectPeripheral:(CBPeripheral *)peripheral
{
    RigLeBaseDevice *deviceToRemove;
    for (RigLeBaseDevice *device in connectedDevices) {
        if (device.peripheral == peripheral) {
            deviceToRemove = device;
            break;
        }
    }
    
    if (deviceToRemove) {
        [connectedDevices removeObject:deviceToRemove];
        [self performSelectorOnMainThread:@selector(reloadTableData:) withObject:nil waitUntilDone:YES];
    }
}

- (void)deviceConnectionDidTimeout:(RigAvailableDeviceData *)device
{
    
}

- (void)deviceConnectionDidFail:(RigAvailableDeviceData *)device
{
    
}

- (void)bluetoothNotPowered
{
    //Do nothing here for now
}

#pragma mark
#pragma mark - RigLeBaseDevice delegate methods
- (void)discoveryDidCompleteForDevice:(RigLeBaseDevice *)device
{
    [self performSelectorOnMainThread:@selector(reloadTableData:) withObject:device waitUntilDone:YES];
}

- (void)didUpdateValueForCharacteristic:(CBCharacteristic *)characteristic forDevice:(RigLeBaseDevice *)device
{
    //Do nothing here for now
}

- (void)didUpdateNotifyStateForCharacteristic:(CBCharacteristic *)characteristic forDevice:(RigLeBaseDevice *)device
{
    //Do nothing here for now
}
@end
