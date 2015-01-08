//
//  DeploymentViewController.m
//  RigablueTest
//
//  Created by Eric P. Stutzenberger on 9/29/14.
//  Copyright (c) 2014 Rigado, LLC. All rights reserved.
//

#import "DeploymentViewController.h"
#import "Rigablue/Rigablue.h"
//#import "Rigablue/RigLeDiscoveryManager.h"
//#import "Rigablue/RigLeConnectionManager.h"
//#import "Rigablue/RigLeBaseDevice.h"
#import "Rigablue/RigFirmwareUpdateManager.h"
#import "SVProgressHUD.h"
#import <CoreBluetooth/CoreBluetooth.h>
#import <Foundation/Foundation.h>
#import <QuartzCore/QuartzCore.h>

#define RIGDFU_SERVICE_ID           @"00001530-1212-efde-1523-785feabcd123"

/* TODO:Add your custom UUIDs here */
#define BMDWARE_SERVICE             @"2413B33F-707F-90BD-2045-2AB8807571B7"
#define BMDWARE_CONTROL_POINT       @"2413B43F-707F-90BD-2045-2AB8807571B7"

#define DIS_SERVICE                 @"180A"
#define MFG_NAME                    @"2A29"
#define FIRMWARE_VERSION            @"2A26"

/* TODO: If you want to confine your updates to some local area close to your device, then leave this number at something high
 * such as -60 or so.  If you don't want that, you can set it to -128 or something. */
#define RSSI_UPDATE_THRESHOLD       -60

/* TODO:Update this command to be the command you want to send to make the application the device reboot in to the bootloader.
 * Whatever this command is, it will be written to the characteristic found in the beginDeployment function.  Make sure
 * to update that function with the approrpiate UUID define.
 */
/* RigCom 0x57305603 */
static uint8_t bootloader_command[] = { 0x03, 0x56, 0x30, 0x57 };

@interface DeploymentViewController () <RigLeDiscoveryManagerDelegate, RigLeConnectionManagerDelegate, RigLeBaseDeviceDelegate, UIPickerViewDataSource, UIPickerViewDelegate, RigFirmwareUpdateManagerDelegate>
{
    BOOL isConnected;
    BOOL isAlreadyBootloader;
    BOOL didUpdateThroughBootloader;
    BOOL isConnectionInProgress;
    BOOL didCompleteAnUpdate;
    
    RigAvailableDeviceData *potentialDevice;
    RigLeBaseDevice *updateDevice;
    
    CBUUID *disServiceUuid;
    CBUUID *mfgNameUuid;
    CBUUID *firmwareVersionUuid;
    
    NSArray *firmwareList;
    NSArray *firmwareBinaryList;
    
    BOOL isUpdateInProgress;
    float currentProgress;
    NSString *currentStatus;
    UIAlertView * updateCompleteAlertView;
    RigFirmwareUpdateManager *updateManager;
    NSString *textFieldData;
    NSString *hudStatus;
    
    uint8_t mac_addr[6];
}
@end

@implementation DeploymentViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    // Do any additional setup after loading the view.
    disServiceUuid = [CBUUID UUIDWithString:DIS_SERVICE];
    mfgNameUuid = [CBUUID UUIDWithString:MFG_NAME];
    firmwareVersionUuid = [CBUUID UUIDWithString:FIRMWARE_VERSION];
    
    [RigLeConnectionManager sharedInstance].delegate = self;
    
    /* TODO: Add your firmware binary to the project.  Then it can be referenced here by name without the extention. */
    firmwareList = [NSArray arrayWithObjects:@"BMDware Blinky", @"BMDware Blinky Secure", @"BMDware Blinky No Key", nil];
    firmwareBinaryList = [NSArray arrayWithObjects:@"bmd-ware-eval", @"bmd-ware-secure", @"bmd-ware-no-key", nil];
    
    CAGradientLayer *bgLayer = [self blueGradient];
    bgLayer.frame = self.view.bounds;
    [self.view.layer insertSublayer:bgLayer atIndex:0];
    [self setNeedsStatusBarAppearanceUpdate];
    [self clearDeviceData];
}

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (void)viewDidAppear:(BOOL)animated
{
    [super viewDidAppear:animated];
    
    NSLog(@"Starting discovery");
    [self startDiscovery];
}

-(UIStatusBarStyle)preferredStatusBarStyle{
    return UIStatusBarStyleLightContent;
}

- (void)startDiscovery
{
    /* TODO: Add custom service UUID to search list.  You can remove BMDware if you want */
    NSArray *uuidList = [NSArray arrayWithObjects:[CBUUID UUIDWithString:RIGDFU_SERVICE_ID], [CBUUID UUIDWithString:BMDWARE_SERVICE], nil];
    RigDeviceRequest *request = [RigDeviceRequest deviceRequestWithUuidList:uuidList timeout:20.0f delegate:self allowDuplicates:NO];
    /* Note: Make find connected devices an option so that order of operations doesn't matter here */
    [[RigLeDiscoveryManager sharedInstance] discoverDevices:request];
    [[RigLeDiscoveryManager sharedInstance] findConnectedDevices:request];
    [self showHudWithStatus:@"Searching..."];
}

- (void)showHudWithStatus:(NSString*)status
{
    [SVProgressHUD showWithStatus:status maskType:SVProgressHUDMaskTypeGradient];
}

- (void)updateHudStatus
{
    [self showHudWithStatus:hudStatus];
}

- (void)clearDeviceData
{
    _deviceNameLabel.text = @"Not Connected";
    _mfgNameLabel.text = @"";
    _firmwareVersionLabel.text = @"";
    _beingDeploymentButton.hidden = YES;
    _deploymentStatus.hidden = YES;
    _deploymentProgress.hidden = YES;
    
    [self reloadPicker];
}

- (void)displayUpdateDeviceData
{
    CBService *disService = nil;
    CBCharacteristic *mfgNameChar = nil;
    CBCharacteristic *fwVersionChar = nil;
    
    [SVProgressHUD dismiss];
    
    for (CBService *service in [updateDevice getSerivceList]) {
        if ([service.UUID isEqual:disServiceUuid]) {
            disService = service;
        }
    }
    
    if (disService == nil) {
        NSLog(@"Device Information Service not found!");
        return;
    }
    
    for (CBCharacteristic *characteristic in [disService characteristics]) {
        if ([characteristic.UUID isEqual:mfgNameUuid]) {
            mfgNameChar = characteristic;
        } else if([characteristic.UUID isEqual:firmwareVersionUuid]) {
            fwVersionChar = characteristic;
        }
    }
    
    _deviceNameLabel.text = updateDevice.name;
    
    _mfgNameLabel.text = [[NSString alloc] initWithData:mfgNameChar.value encoding:NSUTF8StringEncoding];
    _firmwareVersionLabel.text = [[NSString alloc] initWithData:fwVersionChar.value encoding:NSUTF8StringEncoding];
    
    [self reloadPicker];
    
    _beingDeploymentButton.hidden = NO;
    _deploymentProgress.hidden = NO;
    _deploymentProgress.progress = 0.0f;
    _deploymentStatus.hidden = NO;
    _deploymentStatus.text = @"Idle";
}

- (void)reloadPicker
{
    [_deploymentPicker reloadAllComponents];
}

- (void)updateProgressBar
{
    _deploymentProgress.progress = currentProgress;
}

- (void)setUpdateStatus
{
    _deploymentStatus.text = currentStatus;
}

- (void)finalizeUpdate
{
    _deploymentStatus.text = currentStatus;
}

- (IBAction)beginDeploymentPressed:(id)sender
{
    if (updateDevice == nil) {
        //Disable button when device is not available!!
        return;
    }
    
    isUpdateInProgress = YES;
    
    NSData *firmwareImageData;
    NSString *filePath;
    
    NSUInteger row = [_deploymentPicker selectedRowInComponent:0];
    NSString *firmwareFile = [firmwareBinaryList objectAtIndex:row];
    
    /* Load firmware image in to local memory */
    filePath =[[NSBundle mainBundle] pathForResource:firmwareFile ofType:@"bin"];
    firmwareImageData = [NSData dataWithContentsOfFile:filePath];
    
    updateManager = [[RigFirmwareUpdateManager alloc] init];
    updateManager.delegate = self;
    
    if (isAlreadyBootloader) {
        /* This path is for when only the bootloader is present on the device. */
        /* Invoke bootloader here with pointer to binary image of firmware. */
        [updateManager updateFirmware:updateDevice Image:firmwareImageData ImageSize:(uint32_t)firmwareImageData.length activateChar:nil activateCommand:nil activateCommandLen:0];
        isUpdateInProgress = YES;
        return;
    }
    
    CBService *service = nil;
    CBCharacteristic *controlPoint = nil;
    
    /* TODO: Update to use your service and characteristic UUIDs */
    CBUUID *serviceUuid = [CBUUID UUIDWithString:BMDWARE_SERVICE];
    CBUUID *controlPointUuid = [CBUUID UUIDWithString:BMDWARE_CONTROL_POINT];
    
    for (CBService *svc in [updateDevice getSerivceList]) {
        if ([svc.UUID isEqual:serviceUuid]) {
            service = svc;
            break;
        }
    }
    
    if (service != nil) {
        for (CBCharacteristic *characteristic in service.characteristics) {
            if ([characteristic.UUID isEqual:controlPointUuid]) {
                controlPoint = characteristic;
                break;
            }
        }
    }
    
    if (controlPoint != nil) {
        /* Invoke bootloader here with pointer to binary image of firmware. */
        [updateManager updateFirmware:updateDevice Image:firmwareImageData ImageSize:(uint32_t)firmwareImageData.length activateChar:controlPoint activateCommand:bootloader_command activateCommandLen:sizeof(bootloader_command)];
        isUpdateInProgress = YES;
    } else {
        _deploymentStatus.text = @"No Control Point Found!!!";
        isUpdateInProgress = NO;
    }
}

- (void) getMacAddressForDevice:(RigLeBaseDevice*)device
{
    CBUUID *disUuid = [CBUUID UUIDWithString:@"180A"];
    CBUUID *serialNumberUuid = [CBUUID UUIDWithString:@"2A25"];
    
    CBService *service = nil;
    CBCharacteristic *serialNumChar = nil;
    
    /* Find the characteristic */
    for (CBService *svc in [device getSerivceList]) {
        if ([svc.UUID isEqual:disUuid]) {
            service = svc;
            break;
        }
    }
    
    if (service != nil) {
        for (CBCharacteristic *characteristic in service.characteristics) {
            if ([characteristic.UUID isEqual:serialNumberUuid]) {
                serialNumChar = characteristic;
                break;
            }
        }
    }
    
    if (serialNumChar == nil) {
        NSLog(@"Could not find serial number characteristic!!  Mac Address not found!");
        memset(mac_addr, 0xff, sizeof(mac_addr));
        return;
    }
    
    NSData *data = serialNumChar.value;
    
    char *bytes = (char*)data.bytes;
    uint8_t index = 0;
    for (int i = 0; i < data.length; i+=3) {
        if (bytes[i] > 0x39) {
            bytes[i] -= 0x07;
        }
        if (bytes[i+1] > 0x39) {
            bytes[i+1] -= 0x07;
        }
        mac_addr[index] = (bytes[i] - 0x30) << 4;
        mac_addr[index] |= (bytes[i+1] - 0x30);
        index++;
    }
}

//Metallic grey gradient background
- (CAGradientLayer*) greyGradient
{
    
    UIColor *colorOne = [UIColor colorWithWhite:0.9 alpha:1.0];
    UIColor *colorTwo = [UIColor colorWithHue:0.625 saturation:0.0 brightness:0.85 alpha:1.0];
    UIColor *colorThree     = [UIColor colorWithHue:0.625 saturation:0.0 brightness:0.7 alpha:1.0];
    UIColor *colorFour = [UIColor colorWithHue:0.625 saturation:0.0 brightness:0.4 alpha:1.0];
    
    NSArray *colors =  [NSArray arrayWithObjects:(id)colorOne.CGColor, colorTwo.CGColor, colorThree.CGColor, colorFour.CGColor, nil];
    
    NSNumber *stopOne = [NSNumber numberWithFloat:0.0];
    NSNumber *stopTwo = [NSNumber numberWithFloat:0.02];
    NSNumber *stopThree     = [NSNumber numberWithFloat:0.99];
    NSNumber *stopFour = [NSNumber numberWithFloat:1.0];
    
    NSArray *locations = [NSArray arrayWithObjects:stopOne, stopTwo, stopThree, stopFour, nil];
    CAGradientLayer *headerLayer = [CAGradientLayer layer];
    headerLayer.colors = colors;
    headerLayer.locations = locations;
    
    return headerLayer;
    
}

//Blue gradient background
- (CAGradientLayer*) blueGradient
{
    UIColor *colorOne = [UIColor colorWithRed:(100/255.0) green:(115/255.0) blue:(150/255.0) alpha:1.0];
    UIColor *colorTwo = [UIColor colorWithRed:(10/255.0)  green:(29/255.0)  blue:(96/255.0)  alpha:1.0];
    
    NSArray *colors = [NSArray arrayWithObjects:(id)colorOne.CGColor, colorTwo.CGColor, nil];
    NSNumber *stopOne = [NSNumber numberWithFloat:0.0];
    NSNumber *stopTwo = [NSNumber numberWithFloat:1.0];
    
    NSArray *locations = [NSArray arrayWithObjects:stopOne, stopTwo, nil];
    
    CAGradientLayer *headerLayer = [CAGradientLayer layer];
    headerLayer.colors = colors;
    headerLayer.locations = locations;
    
    return headerLayer;
    
}

#pragma mark -
#pragma mark - RigLeDiscoveryDelegate methods
- (void)didDiscoverDevice:(RigAvailableDeviceData *)device
{
    NSLog(@"Discovered a potential deployment device!");
    potentialDevice = device;
    //Display alert depending on if application was running or if dfu was found
    //If dfu was found, ensure the device can be updated with this version and ask user
    //if they would like to update to the latest firmware
    //Otherwise, connect to the device automatically
    //Add call to perform selector on main thread (connect)
    if (isConnectionInProgress) {
        NSLog(@"Connection in progress!");
        return;
    }
    
    /* Here, we ensure the device is in relatively close proximity to the iOS device */
    //TODO: This RSSI check can be removed if you like.  It is here so that I can force update to occur on devices that are local
    //to my desk rather than finding anything in the office that's floating around.
    if (potentialDevice.rssi.integerValue > RSSI_UPDATE_THRESHOLD) {
        if (![potentialDevice.peripheral.name isEqual:@"RigDfu"] && potentialDevice.rssi.integerValue > -60) {
            NSLog(@"Connecting to %@", potentialDevice.peripheral.name);
            isConnectionInProgress = YES;
            isAlreadyBootloader = NO;
            [[RigLeDiscoveryManager sharedInstance] stopDiscoveringDevices];
            [[RigLeConnectionManager sharedInstance] connectDevice:potentialDevice connectionTimeout:5.0f];
            hudStatus = @"Connecting...";
        } else {
            isAlreadyBootloader = YES;
            if (!didCompleteAnUpdate)
            {
                NSLog(@"Connecting to Bootloader");
                isConnectionInProgress = YES;
                [[RigLeDiscoveryManager sharedInstance] stopDiscoveringDevices];
                [NSThread sleepForTimeInterval:0.100];
                [[RigLeConnectionManager sharedInstance] connectDevice:potentialDevice connectionTimeout:5.0f];
                hudStatus = @"Connecting...";
            }
        }
    }
    
    [self performSelectorOnMainThread:@selector(updateHudStatus) withObject:nil waitUntilDone:NO];
}

- (void)discoveryDidTimeout
{
    if (isConnectionInProgress) {
        NSLog(@"Discover timeout occurred, but connection is in progress");
        return;
    }
    [self performSelectorOnMainThread:@selector(startDiscovery) withObject:nil waitUntilDone:NO];
}

- (void)bluetoothNotPowered
{
    //Nothing to do here
}

- (void)didUpdateDeviceData:(RigAvailableDeviceData *)device deviceIndex:(NSUInteger)index
{
    //Don't care about this message
}

#pragma mark -
#pragma mark - RigLeConnectionManagerDelegate methods
- (void)didConnectDevice:(RigLeBaseDevice *)device
{
    updateDevice = device;
    updateDevice.delegate = self;
    [[RigLeDiscoveryManager sharedInstance] stopDiscoveringDevices];
    [updateDevice runDiscovery];
    isConnectionInProgress = NO;
    didCompleteAnUpdate = NO;
    hudStatus = @"Discovering...";
    [self performSelectorOnMainThread:@selector(updateHudStatus) withObject:nil waitUntilDone:NO];
}

- (void)didDisconnectPeripheral:(CBPeripheral *)peripheral
{
    //update connected status
    NSLog(@"Disconnect; attempting reconnection");
    isConnectionInProgress = NO;
    updateDevice = nil;
    [self performSelectorOnMainThread:@selector(clearDeviceData) withObject:nil waitUntilDone:NO];
    if (didUpdateThroughBootloader) {
        [NSThread sleepForTimeInterval:12.0f];
        didUpdateThroughBootloader = NO;
    }
    [self performSelectorOnMainThread:@selector(startDiscovery) withObject:nil waitUntilDone:NO];
}

- (void)deviceConnectionDidFail:(RigAvailableDeviceData *)device
{
    isConnectionInProgress = NO;
}

- (void)deviceConnectionDidTimeout:(RigAvailableDeviceData *)device
{
    isConnectionInProgress = NO;
}

#pragma mark -
#pragma mark - RigLeBaseDeviceDelegate methods
- (void)discoveryDidCompleteForDevice:(RigLeBaseDevice *)device
{
    //Update on-screen information
    //request data from server??
    isConnected = YES;
    [self performSelectorOnMainThread:@selector(displayUpdateDeviceData) withObject:nil waitUntilDone:YES];
}

- (void)didUpdateNotifyStateForCharacteristic:(CBCharacteristic *)characteristic forDevice:(RigLeBaseDevice *)device
{
    
}

- (void)didUpdateValueForCharacteristic:(CBCharacteristic *)characteristic forDevice:(RigLeBaseDevice *)device
{
    
}

- (void)didWriteValueForCharacteristic:(CBCharacteristic *)characteristic forDevice:(RigLeBaseDevice *)device
{
    
}

#pragma mark -
#pragma mark - UIPickerViewDelegate methods
- (NSAttributedString *)pickerView:(UIPickerView *)pickerView attributedTitleForRow:(NSInteger)row forComponent:(NSInteger)component
{
    NSAttributedString *attString;
    NSString *title;
    
    if (updateDevice == nil) {
        title = @"";
    } else {
        title = [firmwareList objectAtIndex:row];
    }
    
    attString = [[NSAttributedString alloc] initWithString:title attributes:@{NSForegroundColorAttributeName:[UIColor whiteColor]}];
    
    return attString;
}

#pragma mark - UIPickerViewDataSource methods
- (NSInteger)numberOfComponentsInPickerView:(UIPickerView *)pickerView
{
    return 1;
}

- (NSInteger)pickerView:(UIPickerView *)pickerView numberOfRowsInComponent:(NSInteger)component
{
    if (updateDevice == nil) {
        return 0;
    }
    
    return firmwareList.count;
}

#pragma mark -
#pragma mark - RigFirmwareUpdateManagerDelegate methods
- (void)updateProgress:(float)progress
{
    currentProgress = progress;
    [self performSelectorOnMainThread:@selector(updateProgressBar) withObject:nil waitUntilDone:YES];
}

- (void)updateStatus:(NSString *)status errorCode:(RigDfuError_t)error
{
    currentStatus = status;
    [self performSelectorOnMainThread:@selector(setUpdateStatus) withObject:nil waitUntilDone:YES];
}

- (void)didFinishUpdate
{
    isUpdateInProgress = NO;
    if (isAlreadyBootloader) {
        didUpdateThroughBootloader = YES;
    }
    isAlreadyBootloader = NO;
    didCompleteAnUpdate = YES;
    
    currentStatus = @"Update Complete!";
    [self performSelectorOnMainThread:@selector(finalizeUpdate) withObject:nil waitUntilDone:NO];
}

- (void)updateFailed:(NSString*)status errorCode:(RigDfuError_t)error
{
    
}
@end
